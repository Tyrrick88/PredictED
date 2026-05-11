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
@Table(name = "note_packs")
public class NotePackEntity {

  @Id
  @Column(length = 80)
  private String id;

  @Column(nullable = false, length = 180)
  private String title;

  @Column(nullable = false, length = 120)
  private String author;

  @Column(name = "price_kes", nullable = false)
  private int priceKes;

  @Column(nullable = false)
  private double rating;

  @Column(nullable = false, length = 40)
  private String tag;

  @Column(nullable = false)
  private boolean verified;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "uploaded_by_user_id")
  private AppUser uploadedBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id")
  private CourseEntity course;

  @Column(name = "original_filename", length = 255)
  private String originalFilename;

  @Column(name = "storage_path", length = 500)
  private String storagePath;

  @Column(name = "content_type", length = 120)
  private String contentType;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  @Column(length = 64)
  private String sha256;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected NotePackEntity() {
  }

  public NotePackEntity(
      String id,
      String title,
      String author,
      int priceKes,
      double rating,
      String tag,
      boolean verified,
      int displayOrder
  ) {
    this(
        id,
        title,
        author,
        priceKes,
        rating,
        tag,
        verified,
        displayOrder,
        null,
        null,
        null,
        null,
        null,
        0,
        null,
        Instant.now()
    );
  }

  public NotePackEntity(
      String id,
      String title,
      String author,
      int priceKes,
      double rating,
      String tag,
      boolean verified,
      int displayOrder,
      AppUser uploadedBy,
      CourseEntity course,
      String originalFilename,
      String storagePath,
      String contentType,
      long sizeBytes,
      String sha256,
      Instant createdAt
  ) {
    this.id = id;
    this.title = title;
    this.author = author;
    this.priceKes = priceKes;
    this.rating = rating;
    this.tag = tag;
    this.verified = verified;
    this.displayOrder = displayOrder;
    this.uploadedBy = uploadedBy;
    this.course = course;
    this.originalFilename = originalFilename;
    this.storagePath = storagePath;
    this.contentType = contentType;
    this.sizeBytes = sizeBytes;
    this.sha256 = sha256;
    this.createdAt = createdAt;
  }

  public String getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getAuthor() {
    return author;
  }

  public int getPriceKes() {
    return priceKes;
  }

  public double getRating() {
    return rating;
  }

  public String getTag() {
    return tag;
  }

  public boolean isVerified() {
    return verified;
  }

  public int getDisplayOrder() {
    return displayOrder;
  }

  public AppUser getUploadedBy() {
    return uploadedBy;
  }

  public CourseEntity getCourse() {
    return course;
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

  public String getSha256() {
    return sha256;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
