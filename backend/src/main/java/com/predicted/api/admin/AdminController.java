package com.predicted.api.admin;

import com.predicted.api.common.AcademicDataService;
import com.predicted.api.common.Models.ModerationItem;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

  private final AcademicDataService academicDataService;

  public AdminController(AcademicDataService academicDataService) {
    this.academicDataService = academicDataService;
  }

  @GetMapping("/moderation")
  @PreAuthorize("hasRole('ADMIN')")
  public List<ModerationItem> moderation() {
    return academicDataService.moderationItems();
  }
}
