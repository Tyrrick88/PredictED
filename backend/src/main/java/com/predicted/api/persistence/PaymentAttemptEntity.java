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
@Table(name = "payment_attempts")
public class PaymentAttemptEntity {

  @Id
  @Column(name = "checkout_request_id", length = 80)
  private String checkoutRequestId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private AppUser user;

  @Column(name = "phone_number", nullable = false, length = 40)
  private String phoneNumber;

  @Column(name = "amount_kes", nullable = false)
  private int amountKes;

  @Column(nullable = false, length = 180)
  private String reference;

  @Column(nullable = false, length = 40)
  private String status;

  @Column(name = "customer_message", nullable = false, length = 300)
  private String customerMessage;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected PaymentAttemptEntity() {
  }

  public PaymentAttemptEntity(
      String checkoutRequestId,
      AppUser user,
      String phoneNumber,
      int amountKes,
      String reference,
      String status,
      String customerMessage,
      Instant createdAt
  ) {
    this.checkoutRequestId = checkoutRequestId;
    this.user = user;
    this.phoneNumber = phoneNumber;
    this.amountKes = amountKes;
    this.reference = reference;
    this.status = status;
    this.customerMessage = customerMessage;
    this.createdAt = createdAt;
  }

  public String getCheckoutRequestId() {
    return checkoutRequestId;
  }

  public String getStatus() {
    return status;
  }

  public String getCustomerMessage() {
    return customerMessage;
  }
}
