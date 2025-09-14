package model;

import java.time.LocalDate;

public class ReviewPlan {
    private int id;
    private int cardId;
    private LocalDate plannedOn;
    private LocalDate reviewedOn;
    private Integer rating;
    private Integer intervalDays;
    private Integer repeats;
    private Double easeFactor;

    // Getter/Setter
    /*



     */
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCardId() { return cardId; }
    public void setCardId(int cardId) { this.cardId = cardId; }

    public LocalDate getPlannedOn() { return plannedOn; }
    public void setPlannedOn(LocalDate plannedOn) { this.plannedOn = plannedOn; }

    public LocalDate getReviewedOn() { return reviewedOn; }
    public void setReviewedOn(LocalDate reviewedOn) { this.reviewedOn = reviewedOn; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public Integer getIntervalDays() { return intervalDays; }
    public void setIntervalDays(Integer intervalDays) { this.intervalDays = intervalDays; }

    public Integer getRepeats() { return repeats; }
    public void setRepeats(Integer repeats) { this.repeats = repeats; }

    public Double getEaseFactor() { return easeFactor; }
    public void setEaseFactor(Double easeFactor) { this.easeFactor = easeFactor; }
}

