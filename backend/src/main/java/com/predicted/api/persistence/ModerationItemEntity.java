package com.predicted.api.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "moderation_items")
public class ModerationItemEntity {

  @Id
  @Column(length = 80)
  private String id;

  @Column(nullable = false, length = 180)
  private String item;

  @Column(nullable = false, length = 60)
  private String type;

  @Column(nullable = false, length = 40)
  private String status;

  @Column(nullable = false, length = 300)
  private String reason;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  protected ModerationItemEntity() {
  }

  public ModerationItemEntity(
      String id,
      String item,
      String type,
      String status,
      String reason,
      int displayOrder
  ) {
    this.id = id;
    this.item = item;
    this.type = type;
    this.status = status;
    this.reason = reason;
    this.displayOrder = displayOrder;
  }

  public String getId() {
    return id;
  }

  public String getItem() {
    return item;
  }

  public String getType() {
    return type;
  }

  public String getStatus() {
    return status;
  }

  public String getReason() {
    return reason;
  }
}
