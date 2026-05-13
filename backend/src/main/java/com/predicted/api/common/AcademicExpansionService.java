package com.predicted.api.common;

import com.predicted.api.common.Models.AcademicModule;
import com.predicted.api.common.Models.AcademicModuleUpsertRequest;
import com.predicted.api.common.Models.AcademicPathDetail;
import com.predicted.api.common.Models.AcademicPathSummary;
import com.predicted.api.common.Models.AcademicPathUpsertRequest;
import com.predicted.api.common.Models.AcademicResource;
import com.predicted.api.common.Models.PlannerAnalytics;
import com.predicted.api.common.Models.PlannerCountdown;
import com.predicted.api.common.Models.PlannerGoal;
import com.predicted.api.common.Models.PlannerMilestoneInput;
import com.predicted.api.common.Models.PlannerReminder;
import com.predicted.api.common.Models.PlannerSetupRequest;
import com.predicted.api.common.Models.SmartPlannerDashboard;
import com.predicted.api.common.Models.SmartPlannerSession;
import com.predicted.api.persistence.AcademicModuleEntity;
import com.predicted.api.persistence.AcademicModuleRepository;
import com.predicted.api.persistence.AcademicPathEntity;
import com.predicted.api.persistence.AcademicPathRepository;
import com.predicted.api.persistence.AcademicResourceEntity;
import com.predicted.api.persistence.AcademicResourceRepository;
import com.predicted.api.persistence.AppUser;
import com.predicted.api.persistence.AppUserRepository;
import com.predicted.api.persistence.PlannerMilestoneEntity;
import com.predicted.api.persistence.PlannerMilestoneRepository;
import com.predicted.api.persistence.PlannerProfileEntity;
import com.predicted.api.persistence.PlannerProfileRepository;
import com.predicted.api.persistence.PlannerSessionEntity;
import com.predicted.api.persistence.PlannerSessionRepository;
import com.predicted.api.upload.DownloadedFile;
import com.predicted.api.upload.FileStorageService;
import com.predicted.api.upload.StoredFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class AcademicExpansionService {

  private static final Set<String> ACADEMIC_CATEGORIES = Set.of(
      "DIPLOMA_PROGRAM",
      "PROFESSIONAL_CERTIFICATION",
      "LANGUAGE_PROGRAM",
      "TECH_BOOTCAMP_SHORT_COURSE",
      "ONLINE_CERTIFICATION_SKILL_TRACK"
  );

  private static final Set<String> DIFFICULTY_LEVELS = Set.of(
      "BEGINNER",
      "INTERMEDIATE",
      "ADVANCED",
      "INTENSIVE"
  );

  private static final Set<String> STAGE_TYPES = Set.of(
      "SEMESTER",
      "LEVEL",
      "STAGE",
      "MODULE",
      "UNIT",
      "CERTIFICATION_STAGE"
  );

  private static final Set<String> MILESTONE_TYPES = Set.of("CAT", "ASSIGNMENT", "QUIZ", "EXAM");
  private static final Set<String> PRIORITY_LEVELS = Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");
  private static final Set<String> PREFERRED_TIMES = Set.of("MORNING", "AFTERNOON", "EVENING", "NIGHT", "FLEXIBLE");

  private final AcademicPathRepository academicPathRepository;
  private final AcademicModuleRepository academicModuleRepository;
  private final AcademicResourceRepository academicResourceRepository;
  private final PlannerProfileRepository plannerProfileRepository;
  private final PlannerMilestoneRepository plannerMilestoneRepository;
  private final PlannerSessionRepository plannerSessionRepository;
  private final AppUserRepository userRepository;
  private final FileStorageService fileStorageService;

  public AcademicExpansionService(
      AcademicPathRepository academicPathRepository,
      AcademicModuleRepository academicModuleRepository,
      AcademicResourceRepository academicResourceRepository,
      PlannerProfileRepository plannerProfileRepository,
      PlannerMilestoneRepository plannerMilestoneRepository,
      PlannerSessionRepository plannerSessionRepository,
      AppUserRepository userRepository,
      FileStorageService fileStorageService
  ) {
    this.academicPathRepository = academicPathRepository;
    this.academicModuleRepository = academicModuleRepository;
    this.academicResourceRepository = academicResourceRepository;
    this.plannerProfileRepository = plannerProfileRepository;
    this.plannerMilestoneRepository = plannerMilestoneRepository;
    this.plannerSessionRepository = plannerSessionRepository;
    this.userRepository = userRepository;
    this.fileStorageService = fileStorageService;
  }

  public List<AcademicPathSummary> academicCatalog(String category, String query) {
    String normalizedCategory = normalizeOptionalCategory(category);
    String normalizedQuery = normalizeSearch(query);
    return academicPathRepository.findAllByOrderByDisplayOrderAsc().stream()
        .filter(AcademicPathEntity::isActive)
        .filter(path -> normalizedCategory == null || path.getCategory().equals(normalizedCategory))
        .filter(path -> matchesSearch(path, normalizedQuery))
        .map(this::toAcademicPathSummary)
        .toList();
  }

  public AcademicPathDetail academicPathDetail(String pathId) {
    return toAcademicPathDetail(requireAcademicPath(pathId));
  }

  public List<AcademicPathDetail> adminAcademicPaths(String category, String query) {
    String normalizedCategory = normalizeOptionalCategory(category);
    String normalizedQuery = normalizeSearch(query);
    return academicPathRepository.findAllByOrderByDisplayOrderAsc().stream()
        .filter(path -> normalizedCategory == null || path.getCategory().equals(normalizedCategory))
        .filter(path -> matchesSearch(path, normalizedQuery))
        .map(this::toAcademicPathDetail)
        .toList();
  }

  @Transactional
  public AcademicPathDetail createAcademicPath(AcademicPathUpsertRequest request) {
    Instant now = Instant.now();
    AcademicPathEntity path = new AcademicPathEntity(
        "ap_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
        normalizeCategory(request.category()),
        cleanText(request.title(), "Title", 180),
        cleanText(request.providerName(), "Provider", 160),
        cleanText(request.duration(), "Duration", 80),
        cleanText(request.description(), "Description", 1200),
        cleanText(request.entryRequirements(), "Entry requirements", 800),
        optionalText(request.structureLabel(), 160),
        joinList(request.careerPaths(), 120),
        normalizeDifficulty(request.difficultyLevel()),
        joinList(request.tags(), 40),
        request.customTrack(),
        request.active(),
        (int) academicPathRepository.count() + 1,
        now,
        now
    );
    return toAcademicPathDetail(academicPathRepository.save(path));
  }

  @Transactional
  public AcademicPathDetail updateAcademicPath(String pathId, AcademicPathUpsertRequest request) {
    AcademicPathEntity path = requireAcademicPath(pathId);
    path.update(
        normalizeCategory(request.category()),
        cleanText(request.title(), "Title", 180),
        cleanText(request.providerName(), "Provider", 160),
        cleanText(request.duration(), "Duration", 80),
        cleanText(request.description(), "Description", 1200),
        cleanText(request.entryRequirements(), "Entry requirements", 800),
        optionalText(request.structureLabel(), 160),
        joinList(request.careerPaths(), 120),
        normalizeDifficulty(request.difficultyLevel()),
        joinList(request.tags(), 40),
        request.customTrack(),
        request.active(),
        Instant.now()
    );
    return toAcademicPathDetail(academicPathRepository.save(path));
  }

  @Transactional
  public void deleteAcademicPath(String pathId) {
    AcademicPathEntity path = requireAcademicPath(pathId);
    path.getResources().forEach(this::deleteStoredResourceFile);
    academicPathRepository.delete(path);
  }

  @Transactional
  public AcademicModule createAcademicModule(String pathId, AcademicModuleUpsertRequest request) {
    AcademicPathEntity path = requireAcademicPath(pathId);
    AcademicModuleEntity module = new AcademicModuleEntity(
        path,
        cleanText(request.title(), "Module title", 180),
        cleanText(request.summary(), "Module summary", 800),
        normalizeStageType(request.stageType()),
        cleanText(request.stageLabel(), "Stage label", 120),
        request.displayOrder()
    );
    return toAcademicModule(academicModuleRepository.save(module));
  }

  @Transactional
  public AcademicModule updateAcademicModule(Long moduleId, AcademicModuleUpsertRequest request) {
    AcademicModuleEntity module = requireAcademicModule(moduleId);
    module.update(
        cleanText(request.title(), "Module title", 180),
        cleanText(request.summary(), "Module summary", 800),
        normalizeStageType(request.stageType()),
        cleanText(request.stageLabel(), "Stage label", 120),
        request.displayOrder()
    );
    return toAcademicModule(academicModuleRepository.save(module));
  }

  @Transactional
  public void deleteAcademicModule(Long moduleId) {
    AcademicModuleEntity module = requireAcademicModule(moduleId);
    List<AcademicResourceEntity> linkedResources = academicResourceRepository.findByModuleIdOrderByDisplayOrderAsc(moduleId);
    linkedResources.forEach(AcademicResourceEntity::detachFromModule);
    academicResourceRepository.saveAll(linkedResources);
    academicModuleRepository.delete(module);
  }

  @Transactional
  public AcademicResource uploadAcademicResource(
      String adminEmail,
      String pathId,
      Long moduleId,
      String title,
      String resourceType,
      String description,
      String externalUrl,
      MultipartFile file
  ) {
    AppUser admin = requireUser(adminEmail);
    AcademicPathEntity path = requireAcademicPath(pathId);
    AcademicModuleEntity module = moduleId == null ? null : requireAcademicModule(moduleId);
    if (module != null && !module.getPath().getId().equals(path.getId())) {
      throw new BadRequestException("Selected module does not belong to this academic path.");
    }
    if (!StringUtils.hasText(externalUrl) && (file == null || file.isEmpty())) {
      throw new BadRequestException("Upload a file or provide an external URL.");
    }
    StoredFile storedFile = (file == null || file.isEmpty()) ? null : fileStorageService.storeNote(file, admin.getId());
    AcademicResourceEntity resource = new AcademicResourceEntity(
        path,
        module,
        admin.getId(),
        cleanText(title, "Resource title", 180),
        cleanText(resourceType, "Resource type", 40).toUpperCase(Locale.ROOT),
        optionalText(description, 500),
        StringUtils.hasText(externalUrl) ? externalUrl.trim() : null,
        storedFile == null ? null : storedFile.originalFilename(),
        storedFile == null ? null : storedFile.storagePath(),
        storedFile == null ? null : storedFile.contentType(),
        storedFile == null ? 0 : storedFile.sizeBytes(),
        academicResourceRepository.findByPathIdOrderByDisplayOrderAsc(pathId).size() + 1,
        Instant.now()
    );
    return toAcademicResource(academicResourceRepository.save(resource));
  }

  @Transactional
  public void deleteAcademicResource(Long resourceId) {
    AcademicResourceEntity resource = requireAcademicResource(resourceId);
    deleteStoredResourceFile(resource);
    academicResourceRepository.delete(resource);
  }

  public DownloadedFile downloadAcademicResource(Long resourceId) {
    AcademicResourceEntity resource = requireAcademicResource(resourceId);
    if (!StringUtils.hasText(resource.getStoragePath())) {
      throw new ResourceNotFoundException("This resource does not have an uploaded file.");
    }
    return new DownloadedFile(
        fileStorageService.load(resource.getStoragePath()),
        resource.getOriginalFilename(),
        resource.getContentType(),
        resource.getSizeBytes()
    );
  }

  public SmartPlannerDashboard plannerDashboard(String email) {
    AppUser user = requireUser(email);
    return plannerProfileRepository.findById(user.getId())
        .map(profile -> {
          List<PlannerMilestoneEntity> milestones = plannerMilestoneRepository.findByUserIdOrderByDueAtAsc(user.getId());
          ensurePlannerSessions(profile, milestones);
          return buildPlannerDashboard(profile, milestones);
        })
        .orElseGet(() -> emptyPlannerDashboard());
  }

  @Transactional
  public SmartPlannerDashboard savePlannerSetup(String email, PlannerSetupRequest request) {
    AppUser user = requireUser(email);
    AcademicPathEntity path = StringUtils.hasText(request.academicPathId())
        ? requireAcademicPath(request.academicPathId())
        : null;
    Instant now = Instant.now();
    PlannerProfileEntity profile = plannerProfileRepository.findById(user.getId()).orElse(null);
    boolean urgencyMode = request.milestones().stream()
        .anyMatch(milestone -> "EXAM".equalsIgnoreCase(milestone.type())
            && milestone.dueAt().isBefore(LocalDateTime.now().plusDays(14)));
    if (profile == null) {
      profile = new PlannerProfileEntity(
          user.getId(),
          path,
          cleanText(request.institutionName(), "Institution", 160),
          cleanText(request.learningPathTitle(), "Learning path", 180),
          request.availableStudyHoursPerDay(),
          joinList(request.weakSubjects(), 120),
          joinList(request.strongSubjects(), 120),
          joinPreferredTimes(request.preferredStudyTimes()),
          urgencyMode,
          request.calendarConnected(),
          now,
          now
      );
    } else {
      profile.update(
          path,
          cleanText(request.institutionName(), "Institution", 160),
          cleanText(request.learningPathTitle(), "Learning path", 180),
          request.availableStudyHoursPerDay(),
          joinList(request.weakSubjects(), 120),
          joinList(request.strongSubjects(), 120),
          joinPreferredTimes(request.preferredStudyTimes()),
          urgencyMode,
          request.calendarConnected(),
          now
      );
    }
    plannerProfileRepository.save(profile);

    plannerMilestoneRepository.deleteByUserId(user.getId());
    List<PlannerMilestoneEntity> milestones = request.milestones().stream()
        .map(item -> new PlannerMilestoneEntity(
            user.getId(),
            normalizeMilestoneType(item.type()),
            cleanText(item.title(), "Milestone title", 180),
            optionalText(item.subjectName(), 180),
            item.dueAt(),
            normalizePriority(item.priority()),
            false
        ))
        .sorted(Comparator.comparing(PlannerMilestoneEntity::getDueAt))
        .toList();
    plannerMilestoneRepository.saveAll(milestones);

    regeneratePlannerSessions(profile, milestones);
    return buildPlannerDashboard(profile, milestones);
  }

  @Transactional
  public SmartPlannerSession completePlannerSession(String email, String sessionId) {
    AppUser user = requireUser(email);
    PlannerSessionEntity session = plannerSessionRepository.findByIdAndUserId(sessionId, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Planner session not found: " + sessionId));
    session.markCompleted();
    return toPlannerSession(plannerSessionRepository.save(session));
  }

  @Transactional
  public SmartPlannerSession reschedulePlannerSession(String email, String sessionId) {
    AppUser user = requireUser(email);
    PlannerSessionEntity session = plannerSessionRepository.findByIdAndUserId(sessionId, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Planner session not found: " + sessionId));
    PlannerProfileEntity profile = plannerProfileRepository.findById(user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Planner profile not found."));
    List<LocalTime> preferredSlots = preferredTimeSlots(splitList(profile.getPreferredStudyTimes()));
    LocalTime nextTime = preferredSlots.contains(session.getScheduledTime())
        ? session.getScheduledTime()
        : preferredSlots.get(0);
    session.reschedule(LocalDate.now().plusDays(1), nextTime);
    return toPlannerSession(plannerSessionRepository.save(session));
  }

  private SmartPlannerDashboard buildPlannerDashboard(
      PlannerProfileEntity profile,
      List<PlannerMilestoneEntity> milestones
  ) {
    List<PlannerSessionEntity> allSessions = plannerSessionRepository
        .findByUserIdOrderBySessionDateAscScheduledTimeAsc(profile.getUserId());
    LocalDate today = LocalDate.now();
    LocalTime nowTime = LocalTime.now();
    List<PlannerMilestoneInput> plannerMilestones = milestones.stream()
        .map(this::toPlannerMilestoneInput)
        .toList();
    List<SmartPlannerSession> todaySessions = allSessions.stream()
        .filter(session -> session.getSessionDate().equals(today))
        .map(this::toPlannerSession)
        .toList();
    List<SmartPlannerSession> upcomingSessions = allSessions.stream()
        .filter(session -> !session.getSessionDate().isBefore(today))
        .limit(10)
        .map(this::toPlannerSession)
        .toList();
    List<PlannerCountdown> countdowns = milestones.stream()
        .filter(milestone -> !milestone.isCompleted())
        .filter(milestone -> !milestone.getDueAt().toLocalDate().isBefore(today))
        .limit(5)
        .map(milestone -> {
          long daysRemaining = ChronoUnit.DAYS.between(today.atStartOfDay(), milestone.getDueAt());
          return new PlannerCountdown(
              milestone.getTitle(),
              milestone.getMilestoneType(),
              milestone.getDueAt(),
              Math.max(0, daysRemaining),
              daysRemaining <= 7 || "CRITICAL".equals(milestone.getPriority())
          );
        })
        .toList();
    List<PlannerReminder> reminders = buildPlannerReminders(today, nowTime, allSessions, milestones);
    PlannerAnalytics analytics = plannerAnalytics(allSessions, milestones);
    List<String> weakSubjects = splitList(profile.getWeakSubjects());
    List<String> strongSubjects = splitList(profile.getStrongSubjects());
    List<String> preferredStudyTimes = splitList(profile.getPreferredStudyTimes());
    List<PlannerGoal> goals = buildPlannerGoals(profile, weakSubjects, milestones, todaySessions);
    return new SmartPlannerDashboard(
        true,
        profile.getInstitutionName(),
        profile.getLearningPathTitle(),
        profile.getAcademicPath() == null ? null : toAcademicPathSummary(profile.getAcademicPath()),
        profile.getAvailableHoursPerDay(),
        weakSubjects,
        strongSubjects,
        preferredStudyTimes,
        profile.isUrgencyMode(),
        profile.isCalendarConnected(),
        plannerMilestones,
        todaySessions,
        upcomingSessions,
        goals,
        countdowns,
        reminders,
        analytics,
        coachRecommendations(profile, weakSubjects, strongSubjects, milestones)
    );
  }

  private SmartPlannerDashboard emptyPlannerDashboard() {
    return new SmartPlannerDashboard(
        false,
        "",
        "",
        null,
        3,
        List.of(),
        List.of(),
        List.of("MORNING", "NIGHT"),
        false,
        false,
        List.of(),
        List.of(),
        List.of(),
        List.of(
            new PlannerGoal("Getting Started", "Tell Predict.ed what you're studying", "Add your institution, path, and deadlines."),
            new PlannerGoal("Workflow", "List all CATs, quizzes, assignments, and exams", "The planner uses those dates to shift revision closer to high-risk weeks.")
        ),
        List.of(),
        List.of(
            new PlannerReminder("Planner setup required", "SETUP", "Add deadlines and study preferences to unlock your adaptive coach.", null, "info")
        ),
        new PlannerAnalytics(0, 0, 0, 0, 0, 0),
        List.of(
            "Start with your nearest deadline so the planner can prioritize urgency accurately.",
            "Add weak subjects explicitly. The coach will bias more sessions toward them.",
            "Choose morning, evening, or night preferences so revision blocks land at the right times."
        )
    );
  }

  private void ensurePlannerSessions(PlannerProfileEntity profile, List<PlannerMilestoneEntity> milestones) {
    LocalDate today = LocalDate.now();
    List<PlannerSessionEntity> existing = plannerSessionRepository
        .findByUserIdAndSessionDateBetweenOrderBySessionDateAscScheduledTimeAsc(profile.getUserId(), today, today.plusDays(6));
    if (existing.isEmpty()) {
      regeneratePlannerSessions(profile, milestones);
    }
  }

  @Transactional
  protected void regeneratePlannerSessions(PlannerProfileEntity profile, List<PlannerMilestoneEntity> milestones) {
    LocalDate today = LocalDate.now();
    plannerSessionRepository.deleteByUserIdAndSessionDateGreaterThanEqualAndCompletedFalse(profile.getUserId(), today);
    List<String> preferredTimes = splitList(profile.getPreferredStudyTimes());
    List<LocalTime> slots = preferredTimeSlots(preferredTimes);
    List<String> focusSubjects = prioritizedSubjects(profile, milestones);
    if (focusSubjects.isEmpty()) {
      focusSubjects = List.of(profile.getLearningPathTitle());
    }

    List<PlannerSessionEntity> sessions = new ArrayList<>();
    int dailyMinutesBudget = Math.max(60, profile.getAvailableHoursPerDay() * 60);
    int sessionMinutes = profile.isUrgencyMode() ? 60 : 50;
    int sessionsPerDay = Math.max(1, Math.min(slots.size(), dailyMinutesBudget / sessionMinutes));
    for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
      LocalDate sessionDate = today.plusDays(dayOffset);
      int remainingMinutes = dailyMinutesBudget;
      for (int slotIndex = 0; slotIndex < sessionsPerDay && remainingMinutes >= 35; slotIndex++) {
        String subject = focusSubjects.get((dayOffset + slotIndex) % focusSubjects.size());
        PlannerMilestoneEntity nearest = nearestMilestoneForSubject(milestones, subject);
        String priority = sessionPriority(dayOffset, nearest, profile.isUrgencyMode());
        String tag = "CRITICAL".equals(priority) ? "Urgency Mode" : dayOffset == 0 ? "Daily Goal" : "Adaptive";
        String title = subject + " revision block";
        String description = nearest == null
            ? "Balance revision using your weak areas, preferred study window, and weekly goals."
            : "Prepare for " + nearest.getTitle() + " by focusing on " + subject + " before "
                + nearest.getDueAt().toLocalDate() + ".";
        int minutes = Math.min(sessionMinutes, remainingMinutes);
        sessions.add(new PlannerSessionEntity(
            "ps_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
            profile.getUserId(),
            sessionDate,
            slots.get(slotIndex),
            tag,
            subject,
            title,
            description,
            minutes,
            priority,
            accentForPriority(priority),
            false,
            false,
            Instant.now()
        ));
        remainingMinutes -= minutes;
      }
    }
    plannerSessionRepository.saveAll(sessions);
  }

  private PlannerAnalytics plannerAnalytics(List<PlannerSessionEntity> sessions, List<PlannerMilestoneEntity> milestones) {
    int planned = sessions.size();
    int completed = (int) sessions.stream().filter(PlannerSessionEntity::isCompleted).count();
    int completionRate = planned == 0 ? 0 : Math.round((completed * 100f) / planned);
    int streakDays = completionStreak(sessions);
    int studyMinutesScheduled = sessions.stream()
        .filter(session -> !session.getSessionDate().isBefore(LocalDate.now()))
        .mapToInt(PlannerSessionEntity::getMinutes)
        .sum();
    int upcomingAssessments = (int) milestones.stream()
        .filter(milestone -> !milestone.isCompleted())
        .filter(milestone -> !milestone.getDueAt().toLocalDate().isBefore(LocalDate.now()))
        .count();
    return new PlannerAnalytics(completed, planned, completionRate, streakDays, upcomingAssessments, studyMinutesScheduled);
  }

  private int completionStreak(List<PlannerSessionEntity> sessions) {
    Set<LocalDate> completedDates = sessions.stream()
        .filter(PlannerSessionEntity::isCompleted)
        .map(PlannerSessionEntity::getSessionDate)
        .collect(Collectors.toSet());
    int streak = 0;
    LocalDate cursor = LocalDate.now();
    while (completedDates.contains(cursor)) {
      streak++;
      cursor = cursor.minusDays(1);
    }
    return streak;
  }

  private List<PlannerReminder> buildPlannerReminders(
      LocalDate today,
      LocalTime nowTime,
      List<PlannerSessionEntity> sessions,
      List<PlannerMilestoneEntity> milestones
  ) {
    List<PlannerReminder> reminders = new ArrayList<>();
    milestones.stream()
        .filter(milestone -> !milestone.isCompleted())
        .filter(milestone -> milestone.getDueAt().isBefore(LocalDateTime.now().plusDays(3)))
        .limit(3)
        .forEach(milestone -> reminders.add(new PlannerReminder(
            milestone.getTitle(),
            milestone.getMilestoneType(),
            milestone.getMilestoneType() + " is coming up soon. Shift revision toward " + defaultSubject(milestone) + ".",
            milestone.getDueAt(),
            milestone.getDueAt().isBefore(LocalDateTime.now().plusDays(1)) ? "urgent" : "warn"
        )));
    sessions.stream()
        .filter(session -> !session.isCompleted())
        .filter(session -> session.getSessionDate().isBefore(today)
            || (session.getSessionDate().equals(today) && session.getScheduledTime().isBefore(nowTime)))
        .limit(2)
        .forEach(session -> reminders.add(new PlannerReminder(
            session.getTitle(),
            "MISSED_SESSION",
            "You missed this study block. Reschedule it to protect your weekly target.",
            LocalDateTime.of(session.getSessionDate(), session.getScheduledTime()),
            "warn"
        )));
    if (reminders.isEmpty()) {
      reminders.add(new PlannerReminder(
          "Daily revision goal",
          "DAILY_GOAL",
          "Keep one focused block for your weakest subject and one lighter review block for retention.",
          null,
          "info"
      ));
    }
    return reminders;
  }

  private List<PlannerGoal> buildPlannerGoals(
      PlannerProfileEntity profile,
      List<String> weakSubjects,
      List<PlannerMilestoneEntity> milestones,
      List<SmartPlannerSession> todaySessions
  ) {
    String weakSubject = weakSubjects.isEmpty() ? profile.getLearningPathTitle() : weakSubjects.get(0);
    PlannerMilestoneEntity nextMilestone = milestones.stream().findFirst().orElse(null);
    return List.of(
        new PlannerGoal("Daily", "Complete today's focused blocks", todaySessions.size() + " sessions scheduled with breaks between them."),
        new PlannerGoal("Weekly", "Bias time toward weak areas", "Give " + weakSubject + " the heaviest revision share this week."),
        new PlannerGoal("Priority", "Prepare for the nearest assessment",
            nextMilestone == null ? "No deadline added yet." : nextMilestone.getTitle() + " is the current lead priority.")
    );
  }

  private List<String> coachRecommendations(
      PlannerProfileEntity profile,
      List<String> weakSubjects,
      List<String> strongSubjects,
      List<PlannerMilestoneEntity> milestones
  ) {
    List<String> recommendations = new ArrayList<>();
    if (!weakSubjects.isEmpty()) {
      recommendations.add("Push " + weakSubjects.get(0) + " into your earliest study block while attention is highest.");
    }
    if (profile.isUrgencyMode()) {
      recommendations.add("Urgency mode is active. Use short, exam-style drills and summary sheets instead of broad reading.");
    }
    milestones.stream()
        .findFirst()
        .ifPresent(milestone -> recommendations.add(
            "Your next deadline is " + milestone.getTitle() + ". Build one revision block around " + defaultSubject(milestone) + " every day until then."
        ));
    recommendations.add("Take a 15-20 minute break after every two focused blocks to reduce burnout and preserve recall.");
    if (!strongSubjects.isEmpty()) {
      recommendations.add("Use " + strongSubjects.get(0) + " as a confidence-recovery block when a heavy day starts to drag.");
    }
    return recommendations.stream().limit(4).toList();
  }

  private void deleteStoredResourceFile(AcademicResourceEntity resource) {
    if (resource == null || !StringUtils.hasText(resource.getStoragePath())) {
      return;
    }
    fileStorageService.delete(resource.getStoragePath());
  }

  private List<String> prioritizedSubjects(PlannerProfileEntity profile, List<PlannerMilestoneEntity> milestones) {
    List<String> subjects = new ArrayList<>();
    milestones.stream()
        .filter(milestone -> !milestone.isCompleted())
        .sorted(Comparator.comparing(PlannerMilestoneEntity::getDueAt))
        .forEach(milestone -> {
          String subject = defaultSubject(milestone);
          subjects.add(subject);
          if (milestone.getDueAt().isBefore(LocalDateTime.now().plusDays(5))) {
            subjects.add(subject);
          }
        });
    splitList(profile.getWeakSubjects()).forEach(subject -> {
      subjects.add(subject);
      subjects.add(subject);
    });
    subjects.addAll(splitList(profile.getStrongSubjects()));
    if (profile.getAcademicPath() != null && subjects.isEmpty()) {
      subjects.addAll(profile.getAcademicPath().getModules().stream()
          .map(AcademicModuleEntity::getTitle)
          .limit(4)
          .toList());
    }
    return new ArrayList<>(new LinkedHashSet<>(subjects));
  }

  private PlannerMilestoneEntity nearestMilestoneForSubject(List<PlannerMilestoneEntity> milestones, String subject) {
    return milestones.stream()
        .filter(milestone -> defaultSubject(milestone).equalsIgnoreCase(subject))
        .min(Comparator.comparing(PlannerMilestoneEntity::getDueAt))
        .orElse(milestones.stream().min(Comparator.comparing(PlannerMilestoneEntity::getDueAt)).orElse(null));
  }

  private String sessionPriority(int dayOffset, PlannerMilestoneEntity milestone, boolean urgencyMode) {
    if (milestone != null) {
      long days = ChronoUnit.DAYS.between(LocalDate.now().atStartOfDay(), milestone.getDueAt());
      if (days <= 2) {
        return "CRITICAL";
      }
      if (days <= 5) {
        return "HIGH";
      }
    }
    if (urgencyMode || dayOffset <= 1) {
      return "HIGH";
    }
    return dayOffset >= 4 ? "MEDIUM" : "HIGH";
  }

  private String defaultSubject(PlannerMilestoneEntity milestone) {
    return StringUtils.hasText(milestone.getSubjectName()) ? milestone.getSubjectName() : milestone.getTitle();
  }

  private List<LocalTime> preferredTimeSlots(List<String> preferredTimes) {
    List<String> normalized = preferredTimes.isEmpty() ? List.of("MORNING", "EVENING") : preferredTimes;
    List<LocalTime> slots = new ArrayList<>();
    for (String time : normalized) {
      switch (time) {
        case "MORNING" -> {
          slots.add(LocalTime.of(6, 30));
          slots.add(LocalTime.of(9, 0));
        }
        case "AFTERNOON" -> {
          slots.add(LocalTime.of(13, 30));
          slots.add(LocalTime.of(16, 0));
        }
        case "EVENING" -> slots.add(LocalTime.of(18, 30));
        case "NIGHT" -> slots.add(LocalTime.of(20, 30));
        default -> slots.add(LocalTime.of(17, 0));
      }
    }
    return slots.isEmpty() ? List.of(LocalTime.of(18, 30)) : new ArrayList<>(new LinkedHashSet<>(slots));
  }

  private AcademicPathSummary toAcademicPathSummary(AcademicPathEntity path) {
    return new AcademicPathSummary(
        path.getId(),
        path.getCategory(),
        path.getTitle(),
        path.getProviderName(),
        path.getDurationLabel(),
        path.getDescription(),
        path.getDifficultyLevel(),
        splitList(path.getTags()),
        path.isCustomTrack(),
        path.getModules().size()
    );
  }

  private AcademicPathDetail toAcademicPathDetail(AcademicPathEntity path) {
    return new AcademicPathDetail(
        path.getId(),
        path.getCategory(),
        path.getTitle(),
        path.getProviderName(),
        path.getDurationLabel(),
        path.getDescription(),
        path.getEntryRequirements(),
        path.getStructureLabel(),
        splitList(path.getCareerOutcomes()),
        path.getDifficultyLevel(),
        splitList(path.getTags()),
        path.isCustomTrack(),
        path.isActive(),
        path.getModules().stream().map(this::toAcademicModule).toList(),
        path.getResources().stream().map(this::toAcademicResource).toList()
    );
  }

  private AcademicModule toAcademicModule(AcademicModuleEntity module) {
    return new AcademicModule(
        module.getId(),
        module.getTitle(),
        module.getSummary(),
        module.getStageType(),
        module.getStageLabel(),
        module.getDisplayOrder()
    );
  }

  private AcademicResource toAcademicResource(AcademicResourceEntity resource) {
    boolean downloadable = StringUtils.hasText(resource.getStoragePath());
    return new AcademicResource(
        resource.getId(),
        resource.getModule() == null ? null : resource.getModule().getId(),
        resource.getTitle(),
        resource.getResourceType(),
        resource.getDescription(),
        resource.getExternalUrl(),
        resource.getOriginalFilename(),
        resource.getCreatedAt(),
        downloadable,
        downloadable ? "/api/catalog/resources/" + resource.getId() + "/download" : null
    );
  }

  private PlannerMilestoneInput toPlannerMilestoneInput(PlannerMilestoneEntity milestone) {
    return new PlannerMilestoneInput(
        milestone.getMilestoneType(),
        milestone.getTitle(),
        milestone.getSubjectName(),
        milestone.getDueAt(),
        milestone.getPriority()
    );
  }

  private SmartPlannerSession toPlannerSession(PlannerSessionEntity session) {
    boolean missed = !session.isCompleted() && (
        session.getSessionDate().isBefore(LocalDate.now())
            || (session.getSessionDate().equals(LocalDate.now()) && session.getScheduledTime().isBefore(LocalTime.now()))
    );
    return new SmartPlannerSession(
        session.getId(),
        session.getSessionDate(),
        session.getScheduledTime(),
        session.getTag(),
        session.getCourse(),
        session.getTitle(),
        session.getDescription(),
        session.getMinutes(),
        session.getPriority(),
        session.isCompleted(),
        missed,
        session.isRescheduled()
    );
  }

  private boolean matchesSearch(AcademicPathEntity path, String query) {
    if (query == null) {
      return true;
    }
    String haystack = Stream.of(
            path.getTitle(),
            path.getProviderName(),
            path.getDescription(),
            path.getTags(),
            path.getCategory(),
            path.getStructureLabel(),
            path.getEntryRequirements(),
            path.getCareerOutcomes()
        )
        .filter(Objects::nonNull)
        .collect(Collectors.joining(" "))
        .toLowerCase(Locale.ROOT);
    return haystack.contains(query);
  }

  private AppUser requireUser(String email) {
    return userRepository.findByEmailIgnoreCase(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
  }

  private AcademicPathEntity requireAcademicPath(String pathId) {
    return academicPathRepository.findById(pathId)
        .orElseThrow(() -> new ResourceNotFoundException("Academic path not found: " + pathId));
  }

  private AcademicModuleEntity requireAcademicModule(Long moduleId) {
    return academicModuleRepository.findById(moduleId)
        .orElseThrow(() -> new ResourceNotFoundException("Academic module not found: " + moduleId));
  }

  private AcademicResourceEntity requireAcademicResource(Long resourceId) {
    return academicResourceRepository.findById(resourceId)
        .orElseThrow(() -> new ResourceNotFoundException("Academic resource not found: " + resourceId));
  }

  private String normalizeCategory(String value) {
    String normalized = cleanText(value, "Category", 60).toUpperCase(Locale.ROOT).replace(' ', '_');
    if (!ACADEMIC_CATEGORIES.contains(normalized)) {
      throw new BadRequestException("Academic category is not supported.");
    }
    return normalized;
  }

  private String normalizeOptionalCategory(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return normalizeCategory(value);
  }

  private String normalizeDifficulty(String value) {
    String normalized = cleanText(value, "Difficulty level", 40).toUpperCase(Locale.ROOT).replace(' ', '_');
    if (!DIFFICULTY_LEVELS.contains(normalized)) {
      throw new BadRequestException("Difficulty level is not supported.");
    }
    return normalized;
  }

  private String normalizeStageType(String value) {
    String normalized = cleanText(value, "Stage type", 40).toUpperCase(Locale.ROOT).replace(' ', '_');
    if (!STAGE_TYPES.contains(normalized)) {
      throw new BadRequestException("Stage type is not supported.");
    }
    return normalized;
  }

  private String normalizeMilestoneType(String value) {
    String normalized = cleanText(value, "Milestone type", 40).toUpperCase(Locale.ROOT).replace(' ', '_');
    if (!MILESTONE_TYPES.contains(normalized)) {
      throw new BadRequestException("Milestone type is not supported.");
    }
    return normalized;
  }

  private String normalizePriority(String value) {
    String normalized = StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "HIGH";
    if (!PRIORITY_LEVELS.contains(normalized)) {
      throw new BadRequestException("Priority is not supported.");
    }
    return normalized;
  }

  private String joinPreferredTimes(List<String> preferredTimes) {
    List<String> normalized = sanitizeList(preferredTimes, 30).stream()
        .map(item -> item.toUpperCase(Locale.ROOT).replace(' ', '_'))
        .peek(item -> {
          if (!PREFERRED_TIMES.contains(item)) {
            throw new BadRequestException("Preferred study time is not supported.");
          }
        })
        .toList();
    return String.join(",", normalized);
  }

  private String joinList(List<String> values, int maxItemLength) {
    return String.join(",", sanitizeList(values, maxItemLength));
  }

  private List<String> sanitizeList(List<String> values, int maxItemLength) {
    if (values == null) {
      return List.of();
    }
    return values.stream()
        .filter(StringUtils::hasText)
        .map(String::trim)
        .map(value -> {
          if (value.length() > maxItemLength) {
            throw new BadRequestException("List item is too long.");
          }
          return value;
        })
        .collect(Collectors.toCollection(LinkedHashSet::new))
        .stream()
        .toList();
  }

  private List<String> splitList(String csv) {
    if (!StringUtils.hasText(csv)) {
      return List.of();
    }
    return List.of(csv.split(",")).stream()
        .map(String::trim)
        .filter(StringUtils::hasText)
        .toList();
  }

  private String cleanText(String value, String field, int maxLength) {
    if (!StringUtils.hasText(value)) {
      throw new BadRequestException(field + " is required.");
    }
    String clean = value.trim();
    if (clean.length() > maxLength) {
      throw new BadRequestException(field + " must be " + maxLength + " characters or fewer.");
    }
    return clean;
  }

  private String optionalText(String value, int maxLength) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    String clean = value.trim();
    if (clean.length() > maxLength) {
      throw new BadRequestException("Field must be " + maxLength + " characters or fewer.");
    }
    return clean;
  }

  private String normalizeSearch(String value) {
    return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
  }

  private String accentForPriority(String priority) {
    return switch (priority) {
      case "CRITICAL" -> "#cf3f4f";
      case "HIGH" -> "#1d4ed8";
      case "MEDIUM" -> "#0f9fbc";
      default -> "#c97800";
    };
  }
}
