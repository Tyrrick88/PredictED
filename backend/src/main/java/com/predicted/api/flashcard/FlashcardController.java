package com.predicted.api.flashcard;

import com.predicted.api.common.AcademicDataService;
import com.predicted.api.common.Models.Flashcard;
import com.predicted.api.common.Models.FlashcardReviewRequest;
import com.predicted.api.common.Models.FlashcardReviewResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/flashcards")
public class FlashcardController {

  private final AcademicDataService academicDataService;

  public FlashcardController(AcademicDataService academicDataService) {
    this.academicDataService = academicDataService;
  }

  @GetMapping("/due")
  public List<Flashcard> due() {
    return academicDataService.dueFlashcards();
  }

  @PostMapping("/{cardId}/review")
  public FlashcardReviewResponse review(
      @PathVariable String cardId,
      @Valid @RequestBody FlashcardReviewRequest request
  ) {
    return academicDataService.reviewFlashcard(cardId, request.rating());
  }
}
