package com.predicted.api.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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
    this.id = id;
    this.title = title;
    this.author = author;
    this.priceKes = priceKes;
    this.rating = rating;
    this.tag = tag;
    this.verified = verified;
    this.displayOrder = displayOrder;
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
}
