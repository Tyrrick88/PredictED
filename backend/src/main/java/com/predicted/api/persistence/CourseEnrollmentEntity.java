package com.predicted.api.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
    name = "course_enrollments",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_course_enrollments_user_course", columnNames = {"user_id", "course_id"})
    },
    indexes = {
        @Index(name = "idx_course_enrollments_user", columnList = "user_id"),
        @Index(name = "idx_course_enrollments_course", columnList = "course_id")
    }
)
public class CourseEnrollmentEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "course_id", nullable = false)
  private CourseEntity course;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected CourseEnrollmentEntity() {
  }

  public CourseEnrollmentEntity(AppUser user, CourseEntity course, Instant createdAt) {
    this.user = user;
    this.course = course;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public AppUser getUser() {
    return user;
  }

  public CourseEntity getCourse() {
    return course;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
