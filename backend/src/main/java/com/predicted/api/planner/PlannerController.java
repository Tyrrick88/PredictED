package com.predicted.api.planner;

import com.predicted.api.common.AcademicDataService;
import com.predicted.api.common.Models.PlannerResponse;
import com.predicted.api.common.Models.StudyTask;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/planner")
public class PlannerController {

  private final AcademicDataService academicDataService;

  public PlannerController(AcademicDataService academicDataService) {
    this.academicDataService = academicDataService;
  }

  @GetMapping("/today")
  public PlannerResponse today(@RequestParam(defaultValue = "3") int focusHours) {
    return academicDataService.planner(focusHours);
  }

  @PostMapping("/tasks/{taskId}/complete")
  public StudyTask complete(@PathVariable String taskId) {
    return academicDataService.completeTask(taskId);
  }
}
