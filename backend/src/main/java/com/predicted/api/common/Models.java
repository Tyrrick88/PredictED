package com.predicted.api.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

public final class Models {

  private Models() {
  }

  public record ApiStatus(
      String app,
      String status,
      String version,
      Instant timestamp
  ) {
  }

  public record UserProfile(
      String id,
      String name,
      String email,
      String university,
      String program,
      String academicLevel,
      String role
  ) {
  }

  public record UpdateProfileRequest(
      @NotBlank @Size(max = 120) String name,
      @NotBlank @Size(max = 140) String university,
      @NotBlank @Size(max = 140) String program,
      @NotBlank @Size(max = 80) String academicLevel
  ) {
  }

  public record UpdateEnrollmentsRequest(
      @NotNull @Size(min = 1) List<@NotBlank String> courseIds
  ) {
  }

  public record ProfileSettings(
      UserProfile profile,
      List<CoursePrediction> enrolledCourses,
      List<CoursePrediction> availableCourses
  ) {
  }

  public record PredictionSummary(
      int score,
      String grade,
      int certainty,
      String risk,
      double expectedGpaLift,
      List<String> focusAreas
  ) {
  }

  public record GamificationSummary(
      int level,
      String title,
      int xp,
      int nextLevelXp,
      int studyStreakDays,
      double studyHours
  ) {
  }

  public record StudyTask(
      String id,
      LocalTime time,
      String tag,
      String course,
      String title,
      String description,
      int minutes,
      String accent,
      String priority,
      boolean completed
  ) {
  }

  public record FeedSignal(
      String id,
      String icon,
      String title,
      String source,
      String body,
      Instant createdAt,
      boolean verified
  ) {
  }

  public record DashboardOverview(
      UserProfile student,
      PredictionSummary prediction,
      GamificationSummary gamification,
      List<StudyTask> tasks,
      List<FeedSignal> feed,
      int flashcardsDue,
      int offlinePacksSynced
  ) {
  }

  public record TopicPrediction(
      String topic,
      int likelihood,
      String weight,
      String recommendedAction
  ) {
  }

  public record CoursePrediction(
      String courseId,
      String courseName,
      String lecturer,
      int certainty,
      int baseScore,
      List<TopicPrediction> topics
  ) {
  }

  public record PredictionInput(
      @Min(0) @Max(100) int revisionAverage,
      @Min(0) @Max(100) int attendance,
      @Min(0) @Max(100) int pastPaperCoverage,
      @Min(0) @Max(100) int assignmentScore
  ) {
  }

  public record PredictionResult(
      String courseId,
      int score,
      String grade,
      String risk,
      int certainty,
      double expectedGpaLift,
      List<TopicPrediction> highYieldTopics
  ) {
  }

  public record MockQuestion(
      int number,
      String courseId,
      String topic,
      String prompt,
      int marks,
      String markingHint
  ) {
  }

  public record PlannerResponse(
      int focusHours,
      int totalMinutes,
      List<StudyTask> tasks
  ) {
  }

  public record TutorRequest(
      @NotBlank String prompt,
      String courseId
  ) {
  }

  public record TutorResponse(
      String answer,
      List<String> nextSteps,
      List<String> generatedFlashcards
  ) {
  }

  public enum FlashcardRating {
    HARD,
    GOOD,
    EASY
  }

  public record Flashcard(
      String id,
      String course,
      String question,
      String answer,
      int dueInHours,
      int mastery
  ) {
  }

  public record FlashcardReviewRequest(
      @NotNull FlashcardRating rating
  ) {
  }

  public record FlashcardReviewResponse(
      String cardId,
      FlashcardRating rating,
      int nextReviewHours,
      int mastery
  ) {
  }

  public record CreateFeedSignalRequest(
      @NotBlank String title,
      @NotBlank String body,
      String source
  ) {
  }

  public record NotePack(
      String id,
      String title,
      String author,
      int priceKes,
      double rating,
      String tag,
      boolean verified
  ) {
  }

  public record ModerationItem(
      String id,
      String item,
      String type,
      String status,
      String reason
  ) {
  }

  public record MpesaPaymentRequest(
      @NotBlank String phoneNumber,
      @Min(1) int amountKes,
      @NotBlank String reference
  ) {
  }

  public record MpesaPaymentResponse(
      String checkoutRequestId,
      String status,
      String customerMessage
  ) {
  }
}
