package com.predicted.api.prediction;

import com.predicted.api.common.AcademicDataService;
import com.predicted.api.common.Models.CoursePrediction;
import com.predicted.api.common.Models.MockQuestion;
import com.predicted.api.common.Models.PredictionInput;
import com.predicted.api.common.Models.PredictionResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/predictions")
public class PredictionController {

  private final AcademicDataService academicDataService;

  public PredictionController(AcademicDataService academicDataService) {
    this.academicDataService = academicDataService;
  }

  @GetMapping("/courses")
  public List<CoursePrediction> courses() {
    return academicDataService.courses();
  }

  @GetMapping("/{courseId}")
  public CoursePrediction course(@PathVariable String courseId) {
    return academicDataService.requireCourse(courseId);
  }

  @PostMapping("/{courseId}/simulate")
  public PredictionResult simulate(
      @PathVariable String courseId,
      @Valid @RequestBody PredictionInput input
  ) {
    return academicDataService.simulate(courseId, input);
  }

  @PostMapping("/{courseId}/mock")
  public List<MockQuestion> mock(@PathVariable String courseId) {
    return academicDataService.generateMock(courseId);
  }
}
