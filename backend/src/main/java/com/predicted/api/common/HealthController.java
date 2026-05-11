package com.predicted.api.common;

import com.predicted.api.common.Models.ApiStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api")
public class HealthController {

  @GetMapping("/health")
  public ApiStatus health() {
    return new ApiStatus("predicted-api", "UP", "0.1.0", Instant.now());
  }
}
