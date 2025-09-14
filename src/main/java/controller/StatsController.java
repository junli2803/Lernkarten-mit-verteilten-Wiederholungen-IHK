package controller;

import dao.CardDAO;
import dao.ReviewStatisticDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.Card;
import model.ReviewStatistic;
import util.StatsUtil;

import java.util.Collections;
import java.util.List;

public class StatsController {

    // Smoothing window (=1 means no smoothing)
    private static final int MA_WINDOW = 2;
    // Fixed review count ticks: 0..10
    private static final int X_MAX_REPS = 10;
    // Time axis upper bound (seconds)
    private static final double TIME_MAX_SECS = 100;

    // UI
    @FXML private Button refreshBtn;
    @FXML private TableView<Row> table;
    @FXML private TableColumn<Row, Integer> colId;
    @FXML private TableColumn<Row, String>  colQuestion;
    @FXML private TableColumn<Row, Integer> colReps;
    @FXML private TableColumn<Row, String>  colAvgTime;
    @FXML private TableColumn<Row, String>  colAvgRating;
    @FXML private TableColumn<Row, String>  colCorrect;

    @FXML private LineChart<Number, Number> ratingChart;
    @FXML private LineChart<Number, Number> timeChart;

    // DAO
    private final CardDAO cardDAO = new CardDAO();
    private final ReviewStatisticDAO statDAO = new ReviewStatisticDAO();

    private final ObservableList<Row> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Table bindings
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colQuestion.setCellValueFactory(new PropertyValueFactory<>("question"));
        colReps.setCellValueFactory(new PropertyValueFactory<>("reps"));
        colAvgTime.setCellValueFactory(new PropertyValueFactory<>("avgTimeText"));
        colAvgRating.setCellValueFactory(new PropertyValueFactory<>("avgRatingText"));
        colCorrect.setCellValueFactory(new PropertyValueFactory<>("correctText"));
        table.setItems(data);

        // Double-click row -> show trends
        table.setRowFactory(tv -> {
            TableRow<Row> r = new TableRow<>();
            r.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !r.isEmpty()) {
                    showTrends(r.getItem().getId());
                }
            });
            return r;
        });

        // Rating chart Y axis: 0~5
        NumberAxis yRating = (NumberAxis) ratingChart.getYAxis();
        yRating.setAutoRanging(false);
        yRating.setLowerBound(0);
        yRating.setUpperBound(5);
        yRating.setTickUnit(1);
        yRating.setLabel("Rating (0–5)");

        // Time chart Y axis: 0~TIME_MAX_SECS seconds
        NumberAxis yTime = (NumberAxis) timeChart.getYAxis();
        yTime.setAutoRanging(false);
        yTime.setLowerBound(0);
        yTime.setUpperBound(TIME_MAX_SECS);
        yTime.setTickUnit(10);
        yTime.setLabel("Time (s)");

        // Lock X axis to 0..10 for both charts
        configXAxis(ratingChart);
        configXAxis(timeChart);

        loadTable(); // initial load
        // refreshBtn.setOnAction(e -> loadTable()); // uncomment if you wire a refresh action
    }

    private void loadTable() {
        data.clear();
        List<Card> cards = cardDAO.findAll();
        for (Card c : cards) {
            int reps = statDAO.countByCard(c.getId());
            double avgDurMs = statDAO.avgDurationMsByCard(c.getId());
            double avgRating = statDAO.avgRatingByCard(c.getId());
            double correctRate = statDAO.correctRateByCard(c.getId());

            String avgTimeText = StatsUtil.prettyDuration((int) Math.round(avgDurMs));
            String avgRatingText = String.format("%.2f", avgRating);
            String correctText = String.format("%.0f%%", correctRate * 100);

            data.add(new Row(c.getId(), c.getQuestion(), reps, avgTimeText, avgRatingText, correctText));
        }

        // Clear charts to avoid stale curves
        ratingChart.getData().clear();
        timeChart.getData().clear();
    }

    private void showTrends(int cardId) {
        List<ReviewStatistic> listAsc = statDAO.findByCardAsc(cardId);
        if (listAsc.isEmpty()) {
            ratingChart.getData().clear();
            timeChart.getData().clear();
            return;
        }

        // Ensure X axis stays fixed 0..10
        configXAxis(ratingChart);
        configXAxis(timeChart);

        // rating + MA
        double[] ratings = listAsc.stream().mapToDouble(ReviewStatistic::getRating).toArray();
        double[] ratingMA = StatsUtil.movingAverage(ratings, MA_WINDOW);

        XYChart.Series<Number, Number> sRating = new XYChart.Series<>();
        sRating.setName("Rating (MA)");
        for (int i = 0; i < ratingMA.length; i++) {
            sRating.getData().add(new XYChart.Data<>(i + 1, ratingMA[i])); // first review at x=1
        }
        ratingChart.getData().setAll(Collections.singletonList(sRating));

        // time(s) + MA
        double[] secs = listAsc.stream().mapToDouble(w -> w.getDurationMs() / 1000.0).toArray();
        double[] secsMA = StatsUtil.movingAverage(secs, MA_WINDOW);

        XYChart.Series<Number, Number> sTime = new XYChart.Series<>();
        sTime.setName("Time (s, MA)");
        for (int i = 0; i < secsMA.length; i++) {
            sTime.getData().add(new XYChart.Data<>(i + 1, secsMA[i]));
        }
        timeChart.getData().setAll(Collections.singletonList(sTime));
    }

    // Fix X axis to 0..10, step 1
    private void configXAxis(LineChart<Number, Number> chart) {
        NumberAxis x = (NumberAxis) chart.getXAxis();
        x.setAutoRanging(false);
        x.setLowerBound(0);
        x.setUpperBound(X_MAX_REPS); // lock to 10
        x.setTickUnit(1);            // 0,1,2,...,10
        x.setMinorTickCount(0);
        x.setLabel("Reviews (0–" + X_MAX_REPS + ")");
    }

    // Simple immutable row bean
    public static class Row {
        private final int id;
        private final String question;
        private final int reps;
        private final String avgTimeText;
        private final String avgRatingText;
        private final String correctText;

        public Row(int id, String question, int reps, String avgTimeText, String avgRatingText, String correctText) {
            this.id = id;
            this.question = question;
            this.reps = reps;
            this.avgTimeText = avgTimeText;
            this.avgRatingText = avgRatingText;
            this.correctText = correctText;
        }
        public int getId() { return id; }
        public String getQuestion() { return question; }
        public int getReps() { return reps; }
        public String getAvgTimeText() { return avgTimeText; }
        public String getAvgRatingText() { return avgRatingText; }
        public String getCorrectText() { return correctText; }
    }
}
