package com.predicted.api.dashboard;

import com.predicted.api.common.AcademicDataService;
import com.predicted.api.common.Models.DashboardOverview;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

  private final AcademicDataService academicDataService;

  public DashboardController(AcademicDataService academicDataService) {
    this.academicDataService = academicDataService;
  }

  @GetMapping("/overview")
  public DashboardOverview overview(Principal principal) {
    return academicDataService.dashboard(principal.getName());
  }
}
