package com.predicted.api.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "topic_predictions")
public class TopicPredictionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "course_id", nullable = false)
  private CourseEntity course;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @Column(nullable = false, length = 140)
  private String topic;

  @Column(nullable = false)
  private int likelihood;

  @Column(nullable = false, length = 30)
  private String weight;

  @Column(name = "recommended_action", nullable = false, length = 300)
  private String recommendedAction;

  protected TopicPredictionEntity() {
  }

  public TopicPredictionEntity(
      int displayOrder,
      String topic,
      int likelihood,
      String weight,
      String recommendedAction
  ) {
    this.displayOrder = displayOrder;
    this.topic = topic;
    this.likelihood = likelihood;
    this.weight = weight;
    this.recommendedAction = recommendedAction;
  }

  void setCourse(CourseEntity course) {
    this.course = course;
  }

  public Long getId() {
    return id;
  }

  public int getDisplayOrder() {
    return displayOrder;
  }

  public String getTopic() {
    return topic;
  }

  public int getLikelihood() {
    return likelihood;
  }

  public String getWeight() {
    return weight;
  }

  public String getRecommendedAction() {
    return recommendedAction;
  }
}
