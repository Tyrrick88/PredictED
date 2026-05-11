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
@Table(name = "feed_signals")
public class FeedSignalEntity {

  @Id
  @Column(length = 80)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_user_id")
  private AppUser createdBy;

  @Column(nullable = false, length = 60)
  private String icon;

  @Column(nullable = false, length = 180)
  private String title;

  @Column(nullable = false, length = 120)
  private String source;

  @Column(nullable = false, length = 1000)
  private String body;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private boolean verified;

  protected FeedSignalEntity() {
  }

  public FeedSignalEntity(
      String id,
      AppUser createdBy,
      String icon,
      String title,
      String source,
      String body,
      Instant createdAt,
      boolean verified
  ) {
    this.id = id;
    this.createdBy = createdBy;
    this.icon = icon;
    this.title = title;
    this.source = source;
    this.body = body;
    this.createdAt = createdAt;
    this.verified = verified;
  }

  public String getId() {
    return id;
  }

  public String getIcon() {
    return icon;
  }

  public String getTitle() {
    return title;
  }

  public String getSource() {
    return source;
  }

  public String getBody() {
    return body;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public boolean isVerified() {
    return verified;
  }
}
