package model;

import java.time.LocalDate;

public class Card {
    private int id;
    private String question;
    private String answer;
    private LocalDate createdAt;

    public Card(String question, String answer, LocalDate createdAt) {
        this.question = question;
        this.answer = answer;
        this.createdAt = createdAt;
    }

    public Card() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }
}

