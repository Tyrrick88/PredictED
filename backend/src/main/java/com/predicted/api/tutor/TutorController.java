package com.predicted.api.tutor;

import com.predicted.api.common.AcademicDataService;
import com.predicted.api.common.BadRequestException;
import com.predicted.api.common.Models.TutorRequest;
import com.predicted.api.common.Models.TutorResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/tutor")
public class TutorController {

  private final AcademicDataService academicDataService;

  public TutorController(AcademicDataService academicDataService) {
    this.academicDataService = academicDataService;
  }

  @PostMapping(value = "/messages", consumes = MediaType.APPLICATION_JSON_VALUE)
  public TutorResponse message(@Valid @RequestBody TutorRequest request, Principal principal) {
    return academicDataService.tutorReply(principal.getName(), request);
  }

  @PostMapping(value = "/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public TutorResponse messageWithNotes(
      @RequestParam String prompt,
      @RequestParam(required = false) String courseId,
      @RequestPart(required = false) List<MultipartFile> notes,
      Principal principal
  ) {
    if (!StringUtils.hasText(prompt)) {
      throw new BadRequestException("Prompt is required.");
    }
    return academicDataService.tutorReply(
        principal.getName(),
        new TutorRequest(prompt.trim(), courseId),
        notes == null ? List.of() : notes
    );
  }
}
