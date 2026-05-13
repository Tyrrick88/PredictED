package com.predicted.api.planner;

import com.predicted.api.common.AcademicDataService;
import com.predicted.api.common.AcademicExpansionService;
import com.predicted.api.common.Models.PlannerResponse;
import com.predicted.api.common.Models.PlannerSetupRequest;
import com.predicted.api.common.Models.SmartPlannerDashboard;
import com.predicted.api.common.Models.SmartPlannerSession;
import com.predicted.api.common.Models.StudyTask;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/planner")
public class PlannerController {

  private final AcademicDataService academicDataService;
  private final AcademicExpansionService academicExpansionService;

  public PlannerController(
      AcademicDataService academicDataService,
      AcademicExpansionService academicExpansionService
  ) {
    this.academicDataService = academicDataService;
    this.academicExpansionService = academicExpansionService;
  }

  @GetMapping("/today")
  public PlannerResponse today(
      @RequestParam(defaultValue = "3") int focusHours,
      Principal principal
  ) {
    return academicDataService.planner(principal.getName(), focusHours);
  }

  @PostMapping("/tasks/{taskId}/complete")
  public StudyTask complete(@PathVariable String taskId, Principal principal) {
    return academicDataService.completeTask(principal.getName(), taskId);
  }

  @GetMapping("/coach")
  public SmartPlannerDashboard coach(Principal principal) {
    return academicExpansionService.plannerDashboard(principal.getName());
  }

  @PutMapping("/coach")
  public SmartPlannerDashboard saveCoachSetup(
      @Valid @RequestBody PlannerSetupRequest request,
      Principal principal
  ) {
    return academicExpansionService.savePlannerSetup(principal.getName(), request);
  }

  @PostMapping("/coach/sessions/{sessionId}/complete")
  public SmartPlannerSession completeCoachSession(@PathVariable String sessionId, Principal principal) {
    return academicExpansionService.completePlannerSession(principal.getName(), sessionId);
  }

  @PostMapping("/coach/sessions/{sessionId}/reschedule")
  public SmartPlannerSession rescheduleCoachSession(@PathVariable String sessionId, Principal principal) {
    return academicExpansionService.reschedulePlannerSession(principal.getName(), sessionId);
  }
}
