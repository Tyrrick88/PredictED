package com.predicted.api.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "academic_paths")
public class AcademicPathEntity {

  @Id
  @Column(length = 64)
  private String id;

  @Column(nullable = false, length = 60)
  private String category;

  @Column(nullable = false, length = 180)
  private String title;

  @Column(name = "provider_name", nullable = false, length = 160)
  private String providerName;

  @Column(name = "duration_label", nullable = false, length = 80)
  private String durationLabel;

  @Column(nullable = false, length = 1200)
  private String description;

  @Column(name = "entry_requirements", nullable = false, length = 800)
  private String entryRequirements;

  @Column(name = "structure_label", length = 160)
  private String structureLabel;

  @Column(name = "career_outcomes", length = 1200)
  private String careerOutcomes;

  @Column(name = "difficulty_level", nullable = false, length = 40)
  private String difficultyLevel;

  @Column(length = 500)
  private String tags;

  @Column(name = "custom_track", nullable = false)
  private boolean customTrack;

  @Column(nullable = false)
  private boolean active;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @OneToMany(mappedBy = "path", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("displayOrder ASC")
  private List<AcademicModuleEntity> modules = new ArrayList<>();

  @OneToMany(mappedBy = "path", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("displayOrder ASC")
  private List<AcademicResourceEntity> resources = new ArrayList<>();

  protected AcademicPathEntity() {
  }

  public AcademicPathEntity(
      String id,
      String category,
      String title,
      String providerName,
      String durationLabel,
      String description,
      String entryRequirements,
      String structureLabel,
      String careerOutcomes,
      String difficultyLevel,
      String tags,
      boolean customTrack,
      boolean active,
      int displayOrder,
      Instant createdAt,
      Instant updatedAt
  ) {
    this.id = id;
    this.category = category;
    this.title = title;
    this.providerName = providerName;
    this.durationLabel = durationLabel;
    this.description = description;
    this.entryRequirements = entryRequirements;
    this.structureLabel = structureLabel;
    this.careerOutcomes = careerOutcomes;
    this.difficultyLevel = difficultyLevel;
    this.tags = tags;
    this.customTrack = customTrack;
    this.active = active;
    this.displayOrder = displayOrder;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public void update(
      String category,
      String title,
      String providerName,
      String durationLabel,
      String description,
      String entryRequirements,
      String structureLabel,
      String careerOutcomes,
      String difficultyLevel,
      String tags,
      boolean customTrack,
      boolean active,
      Instant updatedAt
  ) {
    this.category = category;
    this.title = title;
    this.providerName = providerName;
    this.durationLabel = durationLabel;
    this.description = description;
    this.entryRequirements = entryRequirements;
    this.structureLabel = structureLabel;
    this.careerOutcomes = careerOutcomes;
    this.difficultyLevel = difficultyLevel;
    this.tags = tags;
    this.customTrack = customTrack;
    this.active = active;
    this.updatedAt = updatedAt;
  }

  public String getId() {
    return id;
  }

  public String getCategory() {
    return category;
  }

  public String getTitle() {
    return title;
  }

  public String getProviderName() {
    return providerName;
  }

  public String getDurationLabel() {
    return durationLabel;
  }

  public String getDescription() {
    return description;
  }

  public String getEntryRequirements() {
    return entryRequirements;
  }

  public String getStructureLabel() {
    return structureLabel;
  }

  public String getCareerOutcomes() {
    return careerOutcomes;
  }

  public String getDifficultyLevel() {
    return difficultyLevel;
  }

  public String getTags() {
    return tags;
  }

  public boolean isCustomTrack() {
    return customTrack;
  }

  public boolean isActive() {
    return active;
  }

  public int getDisplayOrder() {
    return displayOrder;
  }

  public void setDisplayOrder(int displayOrder) {
    this.displayOrder = displayOrder;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public List<AcademicModuleEntity> getModules() {
    return modules;
  }

  public List<AcademicResourceEntity> getResources() {
    return resources;
  }
}
