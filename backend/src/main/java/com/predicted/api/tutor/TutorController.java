package com.predicted.api.tutor;

import com.predicted.api.common.AcademicDataService;
import com.predicted.api.common.Models.TutorRequest;
import com.predicted.api.common.Models.TutorResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tutor")
public class TutorController {

  private final AcademicDataService academicDataService;

  public TutorController(AcademicDataService academicDataService) {
    this.academicDataService = academicDataService;
  }

  @PostMapping("/messages")
  public TutorResponse message(@Valid @RequestBody TutorRequest request) {
    return academicDataService.tutorReply(request);
  }
}
