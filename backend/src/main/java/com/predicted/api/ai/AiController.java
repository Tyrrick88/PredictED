package com.predicted.api.ai;

import com.predicted.api.common.Models.AiStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

  private final AcademicAiService academicAiService;

  public AiController(AcademicAiService academicAiService) {
    this.academicAiService = academicAiService;
  }

  @GetMapping("/status")
  public AiStatus status() {
    return academicAiService.status();
  }
}
