package com.predicted.api.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalTime;

@Entity
@Table(name = "study_tasks")
public class StudyTaskEntity {

  @Id
  @Column(length = 80)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @Column(name = "scheduled_time", nullable = false)
  private LocalTime scheduledTime;

  @Column(nullable = false, length = 50)
  private String tag;

  @Column(nullable = false, length = 140)
  private String course;

  @Column(nullable = false, length = 160)
  private String title;

  @Column(nullable = false, length = 500)
  private String description;

  @Column(nullable = false)
  private int minutes;

  @Column(nullable = false, length = 20)
  private String accent;

  @Column(nullable = false, length = 20)
  private String priority;

  @Column(nullable = false)
  private boolean completed;

  protected StudyTaskEntity() {
  }

  public StudyTaskEntity(
      String id,
      AppUser user,
      LocalTime scheduledTime,
      String tag,
      String course,
      String title,
      String description,
      int minutes,
      String accent,
      String priority
  ) {
    this.id = id;
    this.user = user;
    this.scheduledTime = scheduledTime;
    this.tag = tag;
    this.course = course;
    this.title = title;
    this.description = description;
    this.minutes = minutes;
    this.accent = accent;
    this.priority = priority;
    this.completed = false;
  }

  public void markCompleted() {
    this.completed = true;
  }

  public String getId() {
    return id;
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

  public String getAccent() {
    return accent;
  }

  public String getPriority() {
    return priority;
  }

  public boolean isCompleted() {
    return completed;
  }
}
