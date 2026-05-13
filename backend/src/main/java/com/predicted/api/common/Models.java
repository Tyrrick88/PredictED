package com.predicted.api.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

  public record AiStatus(
      String provider,
      String model,
      boolean enabled,
      String mode
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
      boolean verified,
      String courseId,
      String courseName,
      String originalFilename,
      String contentType,
      long sizeBytes,
      Instant createdAt,
      boolean downloadable,
      String downloadUrl
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

  public record AcademicPathSummary(
      String id,
      String category,
      String title,
      String providerName,
      String duration,
      String description,
      String difficultyLevel,
      List<String> tags,
      boolean customTrack,
      int moduleCount
  ) {
  }

  public record AcademicModule(
      Long id,
      String title,
      String summary,
      String stageType,
      String stageLabel,
      int displayOrder
  ) {
  }

  public record AcademicResource(
      Long id,
      Long moduleId,
      String title,
      String resourceType,
      String description,
      String externalUrl,
      String originalFilename,
      Instant createdAt,
      boolean downloadable,
      String downloadUrl
  ) {
  }

  public record AcademicPathDetail(
      String id,
      String category,
      String title,
      String providerName,
      String duration,
      String description,
      String entryRequirements,
      String structureLabel,
      List<String> careerPaths,
      String difficultyLevel,
      List<String> tags,
      boolean customTrack,
      boolean active,
      List<AcademicModule> modules,
      List<AcademicResource> resources
  ) {
  }

  public record AcademicPathUpsertRequest(
      @NotBlank @Size(max = 60) String category,
      @NotBlank @Size(max = 180) String title,
      @NotBlank @Size(max = 160) String providerName,
      @NotBlank @Size(max = 80) String duration,
      @NotBlank @Size(max = 1200) String description,
      @NotBlank @Size(max = 800) String entryRequirements,
      @Size(max = 160) String structureLabel,
      List<@NotBlank @Size(max = 120) String> careerPaths,
      @NotBlank @Size(max = 40) String difficultyLevel,
      List<@NotBlank @Size(max = 40) String> tags,
      boolean customTrack,
      boolean active
  ) {
  }

  public record AcademicModuleUpsertRequest(
      @NotBlank @Size(max = 180) String title,
      @NotBlank @Size(max = 800) String summary,
      @NotBlank @Size(max = 40) String stageType,
      @NotBlank @Size(max = 120) String stageLabel,
      @Min(1) int displayOrder
  ) {
  }

  public record PlannerMilestoneInput(
      @NotBlank @Size(max = 40) String type,
      @NotBlank @Size(max = 180) String title,
      @Size(max = 180) String subjectName,
      @NotNull LocalDateTime dueAt,
      @Size(max = 20) String priority
  ) {
  }

  public record PlannerSetupRequest(
      @NotBlank @Size(max = 160) String institutionName,
      String academicPathId,
      @NotBlank @Size(max = 180) String learningPathTitle,
      @Min(1) @Max(12) int availableStudyHoursPerDay,
      List<@NotBlank @Size(max = 120) String> weakSubjects,
      List<@NotBlank @Size(max = 120) String> strongSubjects,
      List<@NotBlank @Size(max = 30) String> preferredStudyTimes,
      @NotNull List<@Valid PlannerMilestoneInput> milestones,
      boolean calendarConnected
  ) {
  }

  public record SmartPlannerSession(
      String id,
      LocalDate sessionDate,
      LocalTime scheduledTime,
      String tag,
      String course,
      String title,
      String description,
      int minutes,
      String priority,
      boolean completed,
      boolean missed,
      boolean rescheduled
  ) {
  }

  public record PlannerGoal(
      String period,
      String title,
      String target
  ) {
  }

  public record PlannerCountdown(
      String title,
      String type,
      LocalDateTime dueAt,
      long daysRemaining,
      boolean urgent
  ) {
  }

  public record PlannerReminder(
      String title,
      String type,
      String message,
      LocalDateTime dueAt,
      String tone
  ) {
  }

  public record PlannerAnalytics(
      int completedSessions,
      int plannedSessions,
      int completionRate,
      int streakDays,
      int upcomingAssessments,
      int studyMinutesScheduled
  ) {
  }

  public record SmartPlannerDashboard(
      boolean configured,
      String institutionName,
      String learningPathTitle,
      AcademicPathSummary selectedPath,
      int availableStudyHoursPerDay,
      List<String> weakSubjects,
      List<String> strongSubjects,
      List<String> preferredStudyTimes,
      boolean urgencyMode,
      boolean calendarConnected,
      List<PlannerMilestoneInput> milestones,
      List<SmartPlannerSession> todaySessions,
      List<SmartPlannerSession> upcomingSessions,
      List<PlannerGoal> goals,
      List<PlannerCountdown> countdowns,
      List<PlannerReminder> reminders,
      PlannerAnalytics analytics,
      List<String> aiRecommendations
  ) {
  }
}
