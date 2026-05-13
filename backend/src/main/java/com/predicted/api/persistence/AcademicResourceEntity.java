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

import java.time.Instant;

@Entity
@Table(name = "academic_resources")
public class AcademicResourceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "path_id", nullable = false)
  private AcademicPathEntity path;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "module_id")
  private AcademicModuleEntity module;

  @Column(name = "uploaded_by_user_id", length = 64)
  private String uploadedByUserId;

  @Column(nullable = false, length = 180)
  private String title;

  @Column(name = "resource_type", nullable = false, length = 40)
  private String resourceType;

  @Column(length = 500)
  private String description;

  @Column(name = "external_url", length = 500)
  private String externalUrl;

  @Column(name = "original_filename", length = 255)
  private String originalFilename;

  @Column(name = "storage_path", length = 500)
  private String storagePath;

  @Column(name = "content_type", length = 120)
  private String contentType;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected AcademicResourceEntity() {
  }

  public AcademicResourceEntity(
      AcademicPathEntity path,
      AcademicModuleEntity module,
      String uploadedByUserId,
      String title,
      String resourceType,
      String description,
      String externalUrl,
      String originalFilename,
      String storagePath,
      String contentType,
      long sizeBytes,
      int displayOrder,
      Instant createdAt
  ) {
    this.path = path;
    this.module = module;
    this.uploadedByUserId = uploadedByUserId;
    this.title = title;
    this.resourceType = resourceType;
    this.description = description;
    this.externalUrl = externalUrl;
    this.originalFilename = originalFilename;
    this.storagePath = storagePath;
    this.contentType = contentType;
    this.sizeBytes = sizeBytes;
    this.displayOrder = displayOrder;
    this.createdAt = createdAt;
  }

  public void attachToModule(AcademicModuleEntity module) {
    this.module = module;
  }

  public void detachFromModule() {
    this.module = null;
  }

  public Long getId() {
    return id;
  }

  public AcademicPathEntity getPath() {
    return path;
  }

  public AcademicModuleEntity getModule() {
    return module;
  }

  public String getUploadedByUserId() {
    return uploadedByUserId;
  }

  public String getTitle() {
    return title;
  }

  public String getResourceType() {
    return resourceType;
  }

  public String getDescription() {
    return description;
  }

  public String getExternalUrl() {
    return externalUrl;
  }

  public String getOriginalFilename() {
    return originalFilename;
  }

  public String getStoragePath() {
    return storagePath;
  }

  public String getContentType() {
    return contentType;
  }

  public long getSizeBytes() {
    return sizeBytes;
  }

  public int getDisplayOrder() {
    return displayOrder;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
