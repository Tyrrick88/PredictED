package com.predicted.api.marketplace;

import com.predicted.api.common.AcademicDataService;
import com.predicted.api.common.Models.NotePack;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/marketplace")
public class MarketplaceController {

  private final AcademicDataService academicDataService;

  public MarketplaceController(AcademicDataService academicDataService) {
    this.academicDataService = academicDataService;
  }

  @GetMapping("/notes")
  public List<NotePack> notes() {
    return academicDataService.notePacks();
  }
}
