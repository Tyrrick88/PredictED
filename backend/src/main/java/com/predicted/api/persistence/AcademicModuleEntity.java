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
@Table(name = "academic_modules")
public class AcademicModuleEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "path_id", nullable = false)
  private AcademicPathEntity path;

  @Column(nullable = false, length = 180)
  private String title;

  @Column(nullable = false, length = 800)
  private String summary;

  @Column(name = "stage_type", nullable = false, length = 40)
  private String stageType;

  @Column(name = "stage_label", nullable = false, length = 120)
  private String stageLabel;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  protected AcademicModuleEntity() {
  }

  public AcademicModuleEntity(
      AcademicPathEntity path,
      String title,
      String summary,
      String stageType,
      String stageLabel,
      int displayOrder
  ) {
    this.path = path;
    this.title = title;
    this.summary = summary;
    this.stageType = stageType;
    this.stageLabel = stageLabel;
    this.displayOrder = displayOrder;
  }

  public void update(String title, String summary, String stageType, String stageLabel, int displayOrder) {
    this.title = title;
    this.summary = summary;
    this.stageType = stageType;
    this.stageLabel = stageLabel;
    this.displayOrder = displayOrder;
  }

  public Long getId() {
    return id;
  }

  public AcademicPathEntity getPath() {
    return path;
  }

  public String getTitle() {
    return title;
  }

  public String getSummary() {
    return summary;
  }

  public String getStageType() {
    return stageType;
  }

  public String getStageLabel() {
    return stageLabel;
  }

  public int getDisplayOrder() {
    return displayOrder;
  }
}
