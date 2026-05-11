package com.predicted.api.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "flashcards")
public class FlashcardEntity {

  @Id
  @Column(length = 80)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @Column(nullable = false, length = 140)
  private String course;

  @Column(nullable = false, length = 300)
  private String question;

  @Column(nullable = false, length = 600)
  private String answer;

  @Column(name = "due_in_hours", nullable = false)
  private int dueInHours;

  @Column(nullable = false)
  private int mastery;

  protected FlashcardEntity() {
  }

  public FlashcardEntity(
      String id,
      AppUser user,
      String course,
      String question,
      String answer,
      int dueInHours,
      int mastery
  ) {
    this.id = id;
    this.user = user;
    this.course = course;
    this.question = question;
    this.answer = answer;
    this.dueInHours = dueInHours;
    this.mastery = mastery;
  }

  public void applyReview(int nextReviewHours, int updatedMastery) {
    this.dueInHours = nextReviewHours;
    this.mastery = updatedMastery;
  }

  public String getId() {
    return id;
  }

  public String getCourse() {
    return course;
  }

  public String getQuestion() {
    return question;
  }

  public String getAnswer() {
    return answer;
  }

  public int getDueInHours() {
    return dueInHours;
  }

  public int getMastery() {
    return mastery;
  }
}
