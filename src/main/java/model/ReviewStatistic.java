package model;

import java.time.LocalDateTime;

public class ReviewStatistic {
    // allow null for auto-increment on insert
    private final Integer id;
    private final int cardId;
    private final LocalDateTime reviewedAt;
    private final int durationMs;
    private final boolean correct;
    private final int rating;
    private final String notes;

    public ReviewStatistic(Integer id, int cardId, LocalDateTime reviewedAt,
                           int durationMs, boolean correct, int rating, String notes) {
        this.id = id;
        this.cardId = cardId;
        this.reviewedAt = reviewedAt;
        this.durationMs = durationMs;
        this.correct = correct;
        this.rating = rating;
        this.notes = notes;
    }

    // (optional) convenience ctor for inserts without id
    public ReviewStatistic(int cardId, LocalDateTime reviewedAt,
                           int durationMs, boolean correct, int rating, String notes) {
        this(null, cardId, reviewedAt, durationMs, correct, rating, notes);
    }

    public Integer getId() {
        return id;
    }

    public int getCardId() {
        return cardId;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public int getDurationMs() {
        return durationMs;
    }

    public boolean isCorrect() {
        return correct;
    }

    public int getRating() {
        return rating;
    }

    public String getNotes() {
        return notes;
    }
}

