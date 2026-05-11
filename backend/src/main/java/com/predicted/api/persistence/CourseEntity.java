package com.predicted.api.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses")
public class CourseEntity {

  @Id
  @Column(length = 64)
  private String id;

  @Column(name = "course_name", nullable = false, length = 140)
  private String courseName;

  @Column(nullable = false, length = 120)
  private String lecturer;

  @Column(nullable = false)
  private int certainty;

  @Column(name = "base_score", nullable = false)
  private int baseScore;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("displayOrder ASC")
  private List<TopicPredictionEntity> topics = new ArrayList<>();

  protected CourseEntity() {
  }

  public CourseEntity(
      String id,
      String courseName,
      String lecturer,
      int certainty,
      int baseScore,
      int displayOrder
  ) {
    this.id = id;
    this.courseName = courseName;
    this.lecturer = lecturer;
    this.certainty = certainty;
    this.baseScore = baseScore;
    this.displayOrder = displayOrder;
  }

  public void addTopic(TopicPredictionEntity topic) {
    topic.setCourse(this);
    topics.add(topic);
  }

  public String getId() {
    return id;
  }

  public String getCourseName() {
    return courseName;
  }

  public String getLecturer() {
    return lecturer;
  }

  public int getCertainty() {
    return certainty;
  }

  public int getBaseScore() {
    return baseScore;
  }

  public int getDisplayOrder() {
    return displayOrder;
  }

  public List<TopicPredictionEntity> getTopics() {
    return topics;
  }
}
