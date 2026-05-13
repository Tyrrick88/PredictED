package com.predicted.api.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "planner_profiles")
public class PlannerProfileEntity {

  @Id
  @Column(name = "user_id", length = 64)
  private String userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "academic_path_id")
  private AcademicPathEntity academicPath;

  @Column(name = "institution_name", nullable = false, length = 160)
  private String institutionName;

  @Column(name = "learning_path_title", nullable = false, length = 180)
  private String learningPathTitle;

  @Column(name = "available_hours_per_day", nullable = false)
  private int availableHoursPerDay;

  @Column(name = "weak_subjects", length = 600)
  private String weakSubjects;

  @Column(name = "strong_subjects", length = 600)
  private String strongSubjects;

  @Column(name = "preferred_study_times", length = 240)
  private String preferredStudyTimes;

  @Column(name = "urgency_mode", nullable = false)
  private boolean urgencyMode;

  @Column(name = "calendar_connected", nullable = false)
  private boolean calendarConnected;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected PlannerProfileEntity() {
  }

  public PlannerProfileEntity(
      String userId,
      AcademicPathEntity academicPath,
      String institutionName,
      String learningPathTitle,
      int availableHoursPerDay,
      String weakSubjects,
      String strongSubjects,
      String preferredStudyTimes,
      boolean urgencyMode,
      boolean calendarConnected,
      Instant createdAt,
      Instant updatedAt
  ) {
    this.userId = userId;
    this.academicPath = academicPath;
    this.institutionName = institutionName;
    this.learningPathTitle = learningPathTitle;
    this.availableHoursPerDay = availableHoursPerDay;
    this.weakSubjects = weakSubjects;
    this.strongSubjects = strongSubjects;
    this.preferredStudyTimes = preferredStudyTimes;
    this.urgencyMode = urgencyMode;
    this.calendarConnected = calendarConnected;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public void update(
      AcademicPathEntity academicPath,
      String institutionName,
      String learningPathTitle,
      int availableHoursPerDay,
      String weakSubjects,
      String strongSubjects,
      String preferredStudyTimes,
      boolean urgencyMode,
      boolean calendarConnected,
      Instant updatedAt
  ) {
    this.academicPath = academicPath;
    this.institutionName = institutionName;
    this.learningPathTitle = learningPathTitle;
    this.availableHoursPerDay = availableHoursPerDay;
    this.weakSubjects = weakSubjects;
    this.strongSubjects = strongSubjects;
    this.preferredStudyTimes = preferredStudyTimes;
    this.urgencyMode = urgencyMode;
    this.calendarConnected = calendarConnected;
    this.updatedAt = updatedAt;
  }

  public String getUserId() {
    return userId;
  }

  public AcademicPathEntity getAcademicPath() {
    return academicPath;
  }

  public String getInstitutionName() {
    return institutionName;
  }

  public String getLearningPathTitle() {
    return learningPathTitle;
  }

  public int getAvailableHoursPerDay() {
    return availableHoursPerDay;
  }

  public String getWeakSubjects() {
    return weakSubjects;
  }

  public String getStrongSubjects() {
    return strongSubjects;
  }

  public String getPreferredStudyTimes() {
    return preferredStudyTimes;
  }

  public boolean isUrgencyMode() {
    return urgencyMode;
  }

  public boolean isCalendarConnected() {
    return calendarConnected;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
