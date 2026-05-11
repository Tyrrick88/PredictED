package com.predicted.api.feed;

import com.predicted.api.common.AcademicDataService;
import com.predicted.api.common.Models.CreateFeedSignalRequest;
import com.predicted.api.common.Models.FeedSignal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/feed")
public class FeedController {

  private final AcademicDataService academicDataService;

  public FeedController(AcademicDataService academicDataService) {
    this.academicDataService = academicDataService;
  }

  @GetMapping
  public List<FeedSignal> feed() {
    return academicDataService.feed();
  }

  @PostMapping
  public FeedSignal create(@Valid @RequestBody CreateFeedSignalRequest request, Principal principal) {
    return academicDataService.createFeedSignal(principal.getName(), request);
  }
}
