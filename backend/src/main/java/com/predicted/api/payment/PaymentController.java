package com.predicted.api.payment;

import com.predicted.api.common.AcademicDataService;
import com.predicted.api.common.Models.MpesaPaymentRequest;
import com.predicted.api.common.Models.MpesaPaymentResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

  private final AcademicDataService academicDataService;

  public PaymentController(AcademicDataService academicDataService) {
    this.academicDataService = academicDataService;
  }

  @PostMapping("/mpesa/stk-push")
  public MpesaPaymentResponse stkPush(@Valid @RequestBody MpesaPaymentRequest request) {
    return academicDataService.initiateMpesaPayment(request);
  }
}
