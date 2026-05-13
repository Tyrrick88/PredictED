package com.predicted.api.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "planner_sessions")
public class PlannerSessionEntity {

  @Id
  @Column(length = 80)
  private String id;

  @Column(name = "user_id", nullable = false, length = 64)
  private String userId;

  @Column(name = "session_date", nullable = false)
  private LocalDate sessionDate;

  @Column(name = "scheduled_time", nullable = false)
  private LocalTime scheduledTime;

  @Column(nullable = false, length = 50)
  private String tag;

  @Column(nullable = false, length = 180)
  private String course;

  @Column(nullable = false, length = 180)
  private String title;

  @Column(nullable = false, length = 500)
  private String description;

  @Column(nullable = false)
  private int minutes;

  @Column(nullable = false, length = 20)
  private String priority;

  @Column(nullable = false, length = 20)
  private String accent;

  @Column(nullable = false)
  private boolean completed;

  @Column(nullable = false)
  private boolean rescheduled;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected PlannerSessionEntity() {
  }

  public PlannerSessionEntity(
      String id,
      String userId,
      LocalDate sessionDate,
      LocalTime scheduledTime,
      String tag,
      String course,
      String title,
      String description,
      int minutes,
      String priority,
      String accent,
      boolean completed,
      boolean rescheduled,
      Instant createdAt
  ) {
    this.id = id;
    this.userId = userId;
    this.sessionDate = sessionDate;
    this.scheduledTime = scheduledTime;
    this.tag = tag;
    this.course = course;
    this.title = title;
    this.description = description;
    this.minutes = minutes;
    this.priority = priority;
    this.accent = accent;
    this.completed = completed;
    this.rescheduled = rescheduled;
    this.createdAt = createdAt;
  }

  public void markCompleted() {
    this.completed = true;
  }

  public void reschedule(LocalDate sessionDate, LocalTime scheduledTime) {
    this.sessionDate = sessionDate;
    this.scheduledTime = scheduledTime;
    this.rescheduled = true;
  }

  public String getId() {
    return id;
  }

  public String getUserId() {
    return userId;
  }

  public LocalDate getSessionDate() {
    return sessionDate;
  }

  public LocalTime getScheduledTime() {
    return scheduledTime;
  }

  public String getTag() {
    return tag;
  }

  public String getCourse() {
    return course;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public int getMinutes() {
    return minutes;
  }

  public String getPriority() {
    return priority;
  }

  public String getAccent() {
    return accent;
  }

  public boolean isCompleted() {
    return completed;
  }

  public boolean isRescheduled() {
    return rescheduled;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
