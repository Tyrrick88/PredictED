package com.predicted.api.profile;

import com.predicted.api.common.AcademicDataService;
import com.predicted.api.common.Models.ProfileSettings;
import com.predicted.api.common.Models.UpdateEnrollmentsRequest;
import com.predicted.api.common.Models.UpdateProfileRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

  private final AcademicDataService academicDataService;

  public ProfileController(AcademicDataService academicDataService) {
    this.academicDataService = academicDataService;
  }

  @GetMapping
  public ProfileSettings profile(Principal principal) {
    return academicDataService.profileSettings(principal.getName());
  }

  @PutMapping
  public ProfileSettings updateProfile(
      Principal principal,
      @Valid @RequestBody UpdateProfileRequest request
  ) {
    return academicDataService.updateProfile(principal.getName(), request);
  }

  @PutMapping("/courses")
  public ProfileSettings updateCourses(
      Principal principal,
      @Valid @RequestBody UpdateEnrollmentsRequest request
  ) {
    return academicDataService.updateEnrollments(principal.getName(), request);
  }
}
