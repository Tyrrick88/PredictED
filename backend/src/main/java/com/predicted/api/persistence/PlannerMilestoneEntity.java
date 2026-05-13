package com.predicted.api.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "planner_milestones")
public class PlannerMilestoneEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false, length = 64)
  private String userId;

  @Column(name = "milestone_type", nullable = false, length = 40)
  private String milestoneType;

  @Column(nullable = false, length = 180)
  private String title;

  @Column(name = "subject_name", length = 180)
  private String subjectName;

  @Column(name = "due_at", nullable = false)
  private LocalDateTime dueAt;

  @Column(nullable = false, length = 20)
  private String priority;

  @Column(nullable = false)
  private boolean completed;

  protected PlannerMilestoneEntity() {
  }

  public PlannerMilestoneEntity(
      String userId,
      String milestoneType,
      String title,
      String subjectName,
      LocalDateTime dueAt,
      String priority,
      boolean completed
  ) {
    this.userId = userId;
    this.milestoneType = milestoneType;
    this.title = title;
    this.subjectName = subjectName;
    this.dueAt = dueAt;
    this.priority = priority;
    this.completed = completed;
  }

  public Long getId() {
    return id;
  }

  public String getUserId() {
    return userId;
  }

  public String getMilestoneType() {
    return milestoneType;
  }

  public String getTitle() {
    return title;
  }

  public String getSubjectName() {
    return subjectName;
  }

  public LocalDateTime getDueAt() {
    return dueAt;
  }

  public String getPriority() {
    return priority;
  }

  public boolean isCompleted() {
    return completed;
  }
}
