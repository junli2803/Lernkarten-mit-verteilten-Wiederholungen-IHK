package controller;

import dao.CardDAO;
import dao.ReviewPlanDAO;
import dao.ReviewStatisticDAO;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import model.Card;
import model.SM2;
import model.ReviewPlan;
import model.ReviewStatistic;
import util.StatsUtil;

import java.time.LocalDateTime;
import java.util.List;

public class ReviewController {
    private final CardDAO cardDAO = new CardDAO();
    private final ReviewPlanDAO reviewPlanDAO = new ReviewPlanDAO();
    private final ReviewStatisticDAO statDAO = new ReviewStatisticDAO();

    private List<ReviewPlan> todayPlans;
    private int index = 0;
    private int currentCardId;

    private boolean sessionStarted = false;

    // === timer ===
    private AnimationTimer timer;
    private long startNano;          // start time of this card
    private boolean paused = false;  // whether paused
    private long pausedAccumNs = 0;  // accumulated pause duration
    private long pauseStartNs = 0;   // current pause start

    // === FXML ===
    @FXML private Label questionLabel;
    @FXML private TextArea answerArea;
    @FXML private Button showAnswerButton;
    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Label progressLabel;

    @FXML private Button btnQ0, btnQ1, btnQ2, btnQ3, btnQ4, btnQ5;
    @FXML private CheckBox correctCheck;
    @FXML private TextArea notesArea;
    @FXML private Label timerLabel;
    @FXML private Button pauseButton;    // Pause/Resume

    @FXML private Label repetitionsLabel;
    @FXML private Label avgDurLabel;
    @FXML private Label avgRatingLabel;
    @FXML private Label correctRateLabel;

    // -------------------- init --------------------
    @FXML
    public void initialize() {
        if (showAnswerButton != null) showAnswerButton.setOnAction(e -> toggleAnswer(true));
        if (prevButton != null) prevButton.setOnAction(e -> navigate(-1));
        if (nextButton != null) nextButton.setOnAction(e -> navigate(+1));

        if (btnQ0 != null) btnQ0.setOnAction(e -> onRate(0));
        if (btnQ1 != null) btnQ1.setOnAction(e -> onRate(1));
        if (btnQ2 != null) btnQ2.setOnAction(e -> onRate(2));
        if (btnQ3 != null) btnQ3.setOnAction(e -> onRate(3));
        if (btnQ4 != null) btnQ4.setOnAction(e -> onRate(4));
        if (btnQ5 != null) btnQ5.setOnAction(e -> onRate(5));

        toggleAnswer(false);
        questionLabel.setText("Start review timer?");
        disableAll(true);  // initially disabled

        // Global hotkey: SPACE toggles pause/resume
        Platform.runLater(() -> {
            if (pauseButton != null && pauseButton.getScene() != null) {
                Scene scene = pauseButton.getScene();
                scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                    if (e.getCode() == KeyCode.SPACE) {
                        onPauseResume();
                        e.consume();
                    }
                });
            }
        });
    }

    // for MainController to check
    public boolean isSessionRunning() { return sessionStarted; }

    /** Ask whether to start timing */
    public boolean confirmStartTimer() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Start Review?");
        a.setHeaderText("Start timer?");
        a.setContentText("Start this review session and begin timing now.");
        ButtonType startBtn = new ButtonType("Start");
        ButtonType cancelBtn = ButtonType.CANCEL;
        a.getButtonTypes().setAll(startBtn, cancelBtn);
        return a.showAndWait().orElse(cancelBtn) == startBtn;
    }

    /** Start session after confirmation */
    public void startSession() {
        sessionStarted = true;
        loadToday(); // showCurrent() will start stopwatch
    }

    // -------------------- core flow --------------------
    private void loadToday() {
        todayPlans = reviewPlanDAO.getTodayPlans();
        if (todayPlans == null || todayPlans.isEmpty()) {
            questionLabel.setText("No cards for today 🎉");
            disableAll(true);
            return;
        }
        disableAll(false);
        index = Math.min(index, todayPlans.size() - 1);
        showCurrent();
    }

    private void disableAll(boolean disabled) {
        if (showAnswerButton != null) showAnswerButton.setDisable(disabled);
        if (prevButton != null) prevButton.setDisable(disabled);
        if (nextButton != null) nextButton.setDisable(disabled);
        if (btnQ0 != null) btnQ0.setDisable(disabled);
        if (btnQ1 != null) btnQ1.setDisable(disabled);
        if (btnQ2 != null) btnQ2.setDisable(disabled);
        if (btnQ3 != null) btnQ3.setDisable(disabled);
        if (btnQ4 != null) btnQ4.setDisable(disabled);
        if (btnQ5 != null) btnQ5.setDisable(disabled);
        if (pauseButton != null) pauseButton.setDisable(disabled);
    }

    private void showCurrent() {
        // stop previous timer before switching
        stopStopwatch(false);

        ReviewPlan rp = todayPlans.get(index);

        this.currentCardId = rp.getCardId();
        Card c = cardDAO.findById(currentCardId);
        questionLabel.setText(c == null ? "(Card missing)" : c.getQuestion());
        answerArea.setText(c == null ? "" : c.getAnswer());
        answerArea.setScrollTop(0);
        toggleAnswer(false);
        updateProgress();
        refreshStatsView(currentCardId);

        // start timing for this card
        startStopwatch();
    }

    private void updateProgress() {
        if (progressLabel != null && todayPlans != null && !todayPlans.isEmpty()) {
            progressLabel.setText((index + 1) + "/" + todayPlans.size());
        }
    }

    private void toggleAnswer(boolean show) {
        if (answerArea != null) answerArea.setVisible(show);
    }

    private void navigate(int delta) {
        if (todayPlans == null || todayPlans.isEmpty()) return;
        // switch card: stop timer only
        stopStopwatch(false);
        index = (index + delta + todayPlans.size()) % todayPlans.size();
        showCurrent();
    }

    private void onRate(int rating) {
        // stop timer and get effective duration (pause excluded)
        int durationMs = stopStopwatch(true);

        final ReviewPlan cur = todayPlans.get(index);
        final int r = Math.max(0, Math.min(5, rating));
        cur.setRating(r);

        final boolean correct = (correctCheck != null && correctCheck.isSelected());
        final String notes = (notesArea != null) ? notesArea.getText() : null;

        // write statistic
        statDAO.insertReviewStatistic(new ReviewStatistic(
                null, currentCardId, LocalDateTime.now(), durationMs, correct, r, notes
        ));

        // compute next schedule
        if (cur.getIntervalDays() == null) cur.setIntervalDays(1);
        SM2.calculateNext(cur);
        reviewPlanDAO.updatePlanAfterReview(cur, r);

        // next or finish
        if (index + 1 < todayPlans.size()) {
            index++;
            clearReviewInputs();
            showCurrent();
        } else {
            finishSession();
        }
    }

    private void clearReviewInputs() {
        if (notesArea != null) notesArea.clear();
        if (correctCheck != null) correctCheck.setSelected(false);
    }

    private void finishSession() {
        stopStopwatch(false);
        sessionStarted = false;
        disableAll(true);
        questionLabel.setText("All cards reviewed!");
    }

    private void refreshStatsView(int cardId) {
        int count = statDAO.countByCard(cardId);
        double avgDur = statDAO.avgDurationMsByCard(cardId);
        double avgRating = statDAO.avgRatingByCard(cardId);
        double correctRate = statDAO.correctRateByCard(cardId);

        if (repetitionsLabel != null) repetitionsLabel.setText(String.valueOf(count));
        if (avgDurLabel != null) avgDurLabel.setText(StatsUtil.prettyDuration((int) Math.round(avgDur)));
        if (avgRatingLabel != null) avgRatingLabel.setText(String.format("%.2f", avgRating));
        if (correctRateLabel != null) correctRateLabel.setText(String.format("%.0f%%", correctRate * 100));
    }

    // -------------------- timer: start/pause/resume/stop --------------------

    /** start stopwatch for current card and reset pause accumulation */
    private void startStopwatch() {
        startNano = System.nanoTime();
        paused = false;
        pausedAccumNs = 0L;
        pauseStartNs = 0L;
        updatePauseButtonUI();

        if (timer != null) timer.stop();
        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (paused) return; // do not refresh while paused
                long elapsedNs = now - startNano - pausedAccumNs;
                int ms = (int) Math.max(0L, elapsedNs / 1_000_000L);
                if (timerLabel != null) timerLabel.setText(StatsUtil.prettyDuration(ms));
            }
        };
        timer.start();
    }

    /** toggle pause/resume (button & Space hotkey) */
    @FXML
    public void onPauseResume() {
        if (pauseButton == null) return;
        if (!paused) {
            // enter pause
            paused = true;
            pauseStartNs = System.nanoTime();
        } else {
            // resume from pause
            long now = System.nanoTime();
            paused = false;
            pausedAccumNs += now - pauseStartNs;
            pauseStartNs = 0L;
        }
        updatePauseButtonUI();
    }

    /** update pause button text */
    private void updatePauseButtonUI() {
        if (pauseButton == null) return;
        pauseButton.setText(paused ? "Resume ⏵" : "Pause ⏸");
    }

    /**
     * Stop the stopwatch.
     * @param returnDuration whether to compute and return effective duration (ms)
     * @return effective duration (ms), or 0 if returnDuration=false
     */
    private int stopStopwatch(boolean returnDuration) {
        if (timer != null) timer.stop();

        int ms = 0;
        if (returnDuration) {
            long now = System.nanoTime();
            if (paused) {
                // if still paused, count this pause segment as well
                pausedAccumNs += now - pauseStartNs;
                paused = false;
                pauseStartNs = 0L;
            }
            long elapsedNs = now - startNano - pausedAccumNs;
            ms = (int) Math.max(0L, elapsedNs / 1_000_000L);
            if (timerLabel != null) timerLabel.setText(StatsUtil.prettyDuration(ms));
        }
        updatePauseButtonUI();
        return ms;
    }

    /** stop only, no duration calculation (kept for compatibility) */
    private void stopStopwatch(boolean returnDuration, boolean dummy) {
        if (timer != null) timer.stop();
    }
}
