package com.predicted.api.common;

import com.predicted.api.common.Models.CoursePrediction;
import com.predicted.api.common.Models.CreateFeedSignalRequest;
import com.predicted.api.common.Models.DashboardOverview;
import com.predicted.api.common.Models.FeedSignal;
import com.predicted.api.common.Models.Flashcard;
import com.predicted.api.common.Models.FlashcardRating;
import com.predicted.api.common.Models.FlashcardReviewResponse;
import com.predicted.api.common.Models.GamificationSummary;
import com.predicted.api.common.Models.MockQuestion;
import com.predicted.api.common.Models.ModerationItem;
import com.predicted.api.common.Models.MpesaPaymentRequest;
import com.predicted.api.common.Models.MpesaPaymentResponse;
import com.predicted.api.common.Models.NotePack;
import com.predicted.api.common.Models.PlannerResponse;
import com.predicted.api.common.Models.PredictionInput;
import com.predicted.api.common.Models.PredictionResult;
import com.predicted.api.common.Models.PredictionSummary;
import com.predicted.api.common.Models.ProfileSettings;
import com.predicted.api.common.Models.StudyTask;
import com.predicted.api.common.Models.TopicPrediction;
import com.predicted.api.common.Models.TutorRequest;
import com.predicted.api.common.Models.TutorResponse;
import com.predicted.api.common.Models.UpdateEnrollmentsRequest;
import com.predicted.api.common.Models.UpdateProfileRequest;
import com.predicted.api.common.Models.UserProfile;
import com.predicted.api.persistence.AppUser;
import com.predicted.api.persistence.AppUserRepository;
import com.predicted.api.persistence.CourseEnrollmentEntity;
import com.predicted.api.persistence.CourseEnrollmentRepository;
import com.predicted.api.persistence.CourseEntity;
import com.predicted.api.persistence.CourseRepository;
import com.predicted.api.persistence.FeedSignalEntity;
import com.predicted.api.persistence.FeedSignalRepository;
import com.predicted.api.persistence.FlashcardEntity;
import com.predicted.api.persistence.FlashcardRepository;
import com.predicted.api.persistence.ModerationItemEntity;
import com.predicted.api.persistence.ModerationItemRepository;
import com.predicted.api.persistence.NotePackEntity;
import com.predicted.api.persistence.NotePackRepository;
import com.predicted.api.persistence.PaymentAttemptEntity;
import com.predicted.api.persistence.PaymentAttemptRepository;
import com.predicted.api.persistence.StudyTaskEntity;
import com.predicted.api.persistence.StudyTaskRepository;
import com.predicted.api.persistence.TopicPredictionEntity;
import com.predicted.api.persistence.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Transactional(readOnly = true)
public class AcademicDataService {

  private final AppUserRepository userRepository;
  private final CourseRepository courseRepository;
  private final CourseEnrollmentRepository courseEnrollmentRepository;
  private final StudyTaskRepository studyTaskRepository;
  private final FeedSignalRepository feedSignalRepository;
  private final FlashcardRepository flashcardRepository;
  private final NotePackRepository notePackRepository;
  private final ModerationItemRepository moderationItemRepository;
  private final PaymentAttemptRepository paymentAttemptRepository;

  public AcademicDataService(
      AppUserRepository userRepository,
      CourseRepository courseRepository,
      CourseEnrollmentRepository courseEnrollmentRepository,
      StudyTaskRepository studyTaskRepository,
      FeedSignalRepository feedSignalRepository,
      FlashcardRepository flashcardRepository,
      NotePackRepository notePackRepository,
      ModerationItemRepository moderationItemRepository,
      PaymentAttemptRepository paymentAttemptRepository
  ) {
    this.userRepository = userRepository;
    this.courseRepository = courseRepository;
    this.courseEnrollmentRepository = courseEnrollmentRepository;
    this.studyTaskRepository = studyTaskRepository;
    this.feedSignalRepository = feedSignalRepository;
    this.flashcardRepository = flashcardRepository;
    this.notePackRepository = notePackRepository;
    this.moderationItemRepository = moderationItemRepository;
    this.paymentAttemptRepository = paymentAttemptRepository;
  }

  public DashboardOverview dashboard(String email) {
    List<CourseEntity> enrolledCourses = enrolledCourseEntities(email);
    CourseEntity focusCourse = enrolledCourses.stream()
        .findFirst()
        .orElseGet(() -> requireCourseEntity("distributed"));
    List<TopicPrediction> topics = mapTopics(focusCourse);
    PredictionSummary prediction = new PredictionSummary(
        focusCourse.getBaseScore(),
        gradeFor(focusCourse.getBaseScore()),
        focusCourse.getCertainty(),
        riskFor(focusCourse.getBaseScore()),
        0.20,
        topics.stream().limit(3).map(TopicPrediction::topic).toList()
    );

    return new DashboardOverview(
        requireUser(email).toProfile(),
        prediction,
        new GamificationSummary(24, "Senior Scholar", 12450, 15000, 14, 128.5),
        planner(email, 3).tasks(),
        feed(),
        dueFlashcards(email).size(),
        Math.max(1, enrolledCourses.size())
    );
  }

  public UserProfile profile(String email) {
    return requireUser(email).toProfile();
  }

  public ProfileSettings profileSettings(String email) {
    AppUser user = requireUser(email);
    return new ProfileSettings(
        user.toProfile(),
        enrolledCourseEntities(user).stream().map(this::toCoursePrediction).toList(),
        allCourseEntities().stream().map(this::toCoursePrediction).toList()
    );
  }

  @Transactional
  public ProfileSettings updateProfile(String email, UpdateProfileRequest request) {
    AppUser user = requireUser(email);
    user.updateProfile(
        request.name().trim(),
        request.university().trim(),
        request.program().trim(),
        request.academicLevel().trim()
    );
    userRepository.save(user);
    return profileSettings(email);
  }

  @Transactional
  public ProfileSettings updateEnrollments(String email, UpdateEnrollmentsRequest request) {
    AppUser user = requireUser(email);
    List<String> requestedIds = request.courseIds().stream()
        .map(String::trim)
        .map(id -> id.toLowerCase(Locale.ROOT))
        .filter(id -> !id.isBlank())
        .collect(Collectors.toCollection(LinkedHashSet::new))
        .stream()
        .toList();
    if (requestedIds.isEmpty()) {
      throw new ConflictException("Choose at least one course.");
    }

    List<CourseEntity> courses = courseRepository.findAllById(requestedIds);
    if (courses.size() != requestedIds.size()) {
      throw new ResourceNotFoundException("One or more selected courses were not found.");
    }
    Map<String, CourseEntity> byId = courses.stream()
        .collect(Collectors.toMap(CourseEntity::getId, course -> course));

    courseEnrollmentRepository.deleteByUser(user);
    courseEnrollmentRepository.flush();
    requestedIds.forEach(courseId -> courseEnrollmentRepository.save(
        new CourseEnrollmentEntity(user, byId.get(courseId), Instant.now())
    ));
    return profileSettings(email);
  }

  public List<CoursePrediction> courses() {
    return allCourseEntities()
        .stream()
        .map(this::toCoursePrediction)
        .toList();
  }

  public List<CoursePrediction> courses(String email) {
    return enrolledCourseEntities(email)
        .stream()
        .map(this::toCoursePrediction)
        .toList();
  }

  public CoursePrediction requireCourse(String courseId) {
    return toCoursePrediction(requireCourseEntity(courseId));
  }

  public CoursePrediction requireCourse(String email, String courseId) {
    return toCoursePrediction(requireEnrolledCourseEntity(email, courseId));
  }

  public PredictionResult simulate(String email, String courseId, PredictionInput input) {
    CourseEntity course = requireEnrolledCourseEntity(email, courseId);
    int score = Math.round(
        (float) ((input.revisionAverage() * 0.36)
            + (input.attendance() * 0.16)
            + (input.pastPaperCoverage() * 0.28)
            + (input.assignmentScore() * 0.20))
    );
    double expectedGpaLift = Math.round(((score - course.getBaseScore()) / 100.0) * 100.0) / 100.0;

    return new PredictionResult(
        course.getId(),
        score,
        gradeFor(score),
        riskFor(score),
        certaintyFor(input, course.getCertainty()),
        expectedGpaLift,
        mapTopics(course).stream()
            .sorted(Comparator.comparing(TopicPrediction::likelihood).reversed())
            .limit(3)
            .toList()
    );
  }

  public List<MockQuestion> generateMock(String courseId) {
    CoursePrediction course = requireCourse(courseId);
    List<TopicPrediction> topics = course.topics();
    return IntStream.range(0, Math.min(3, topics.size()))
        .mapToObj(index -> {
          TopicPrediction topic = topics.get(index);
          String prompt = switch (index) {
            case 0 -> "Explain " + topic.topic() + " using a lecturer-style worked example.";
            case 1 -> "Compare two approaches related to " + topic.topic() + " and justify the stronger answer.";
            default -> "Solve a short scenario involving " + topic.topic() + " and show each step.";
          };
          return new MockQuestion(
              index + 1,
              course.courseId(),
              topic.topic(),
              prompt,
              index == 0 ? 12 : 8,
              topic.recommendedAction()
          );
        })
        .toList();
  }

  public List<MockQuestion> generateMock(String email, String courseId) {
    CoursePrediction course = requireCourse(email, courseId);
    List<TopicPrediction> topics = course.topics();
    return IntStream.range(0, Math.min(3, topics.size()))
        .mapToObj(index -> {
          TopicPrediction topic = topics.get(index);
          String prompt = switch (index) {
            case 0 -> "Explain " + topic.topic() + " using a lecturer-style worked example.";
            case 1 -> "Compare two approaches related to " + topic.topic() + " and justify the stronger answer.";
            default -> "Solve a short scenario involving " + topic.topic() + " and show each step.";
          };
          return new MockQuestion(
              index + 1,
              course.courseId(),
              topic.topic(),
              prompt,
              index == 0 ? 12 : 8,
              topic.recommendedAction()
          );
        })
        .toList();
  }

  public PlannerResponse planner(String email, int focusHours) {
    int boundedHours = Math.max(1, Math.min(5, focusHours));
    Set<String> enrolledCourseNames = enrolledCourseNames(email);
    List<StudyTaskEntity> availableTasks = studyTaskRepository.findByUserEmailIgnoreCaseOrderByScheduledTimeAsc(email)
        .stream()
        .filter(task -> enrolledCourseNames.isEmpty() || enrolledCourseNames.contains(task.getCourse()))
        .toList();
    int visibleTasks = Math.min(availableTasks.size(), Math.max(2, boundedHours + 1));
    List<StudyTask> tasks = availableTasks.stream()
        .limit(visibleTasks)
        .map(this::toStudyTask)
        .toList();
    int totalMinutes = tasks.stream().mapToInt(StudyTask::minutes).sum();
    return new PlannerResponse(boundedHours, totalMinutes, tasks);
  }

  @Transactional
  public StudyTask completeTask(String email, String taskId) {
    StudyTaskEntity task = studyTaskRepository.findByIdAndUserEmailIgnoreCase(taskId, email)
        .orElseThrow(() -> new ResourceNotFoundException("Study task not found: " + taskId));
    task.markCompleted();
    return toStudyTask(studyTaskRepository.save(task));
  }

  public TutorResponse tutorReply(TutorRequest request) {
    String prompt = request.prompt().toLowerCase();
    String answer;
    List<String> nextSteps;
    List<String> generatedCards;

    if (prompt.contains("vector") || prompt.contains("distributed")) {
      answer = "For vector clocks, state the rule first: each process keeps one counter per process, increments on local/send events, and merges by max on receive before incrementing. In exam answers, finish by comparing two timestamps to show causality or concurrency.";
      nextSteps = List.of("Draw a 3-process event timeline", "Compare two vector timestamps", "Attempt one past-paper causality question");
      generatedCards = List.of("When are two vector-clock events concurrent?", "What happens on receive in vector clocks?");
    } else if (prompt.contains("bayes") || prompt.contains("network")) {
      answer = "Start from the graph structure, identify parents, then apply d-separation before computing probabilities. The highest-scoring answers show both the independence reasoning and the numeric factorization.";
      nextSteps = List.of("List parent nodes", "Mark observed evidence", "Factorize the joint probability");
      generatedCards = List.of("What does d-separation test?", "How is a Bayesian network factorized?");
    } else if (prompt.contains("ll") || prompt.contains("parsing") || prompt.contains("compiler")) {
      answer = "For LL(1), calculate FIRST and FOLLOW sets, then fill the parsing table. If any cell has more than one production, the grammar is not LL(1).";
      nextSteps = List.of("Compute FIRST sets", "Compute FOLLOW sets", "Check parsing-table conflicts");
      generatedCards = List.of("What causes an LL(1) conflict?", "Why does FOLLOW matter for epsilon productions?");
    } else {
      answer = "I would study that with a 20-minute loop: define the concept, solve one lecturer-style example, then turn each mistake into a flashcard.";
      nextSteps = List.of("Pick a course", "Solve one short question", "Review related flashcards");
      generatedCards = List.of("What is the key definition?", "What mistake did I make last time?");
    }

    return new TutorResponse(answer, nextSteps, generatedCards);
  }

  public List<Flashcard> dueFlashcards(String email) {
    Set<String> enrolledCourseNames = enrolledCourseNames(email);
    return flashcardRepository.findByUserEmailIgnoreCaseOrderByDueInHoursAscIdAsc(email)
        .stream()
        .filter(card -> enrolledCourseNames.isEmpty() || enrolledCourseNames.contains(card.getCourse()))
        .map(this::toFlashcard)
        .toList();
  }

  @Transactional
  public FlashcardReviewResponse reviewFlashcard(String email, String cardId, FlashcardRating rating) {
    FlashcardEntity card = flashcardRepository.findByIdAndUserEmailIgnoreCase(cardId, email)
        .orElseThrow(() -> new ResourceNotFoundException("Flashcard not found: " + cardId));
    int updated = switch (rating) {
      case HARD -> Math.max(0, card.getMastery() - 5);
      case GOOD -> Math.min(100, card.getMastery() + 8);
      case EASY -> Math.min(100, card.getMastery() + 14);
    };
    int nextReviewHours = switch (rating) {
      case HARD -> 4;
      case GOOD -> 24;
      case EASY -> 72;
    };
    card.applyReview(nextReviewHours, updated);
    flashcardRepository.save(card);
    return new FlashcardReviewResponse(card.getId(), rating, nextReviewHours, updated);
  }

  public List<FeedSignal> feed() {
    return feedSignalRepository.findAllByOrderByCreatedAtDesc()
        .stream()
        .map(this::toFeedSignal)
        .toList();
  }

  @Transactional
  public FeedSignal createFeedSignal(String email, CreateFeedSignalRequest request) {
    AppUser creator = userRepository.findByEmailIgnoreCase(email).orElse(null);
    FeedSignalEntity signal = feedSignalRepository.save(new FeedSignalEntity(
        "sig_" + UUID.randomUUID().toString().substring(0, 8),
        creator,
        "bolt",
        request.title(),
        request.source() == null || request.source().isBlank() ? "Student Signal" : request.source(),
        request.body(),
        Instant.now(),
        false
    ));
    return toFeedSignal(signal);
  }

  public List<NotePack> notePacks() {
    return notePackRepository.findAllByOrderByDisplayOrderAsc()
        .stream()
        .map(this::toNotePack)
        .toList();
  }

  public List<ModerationItem> moderationItems() {
    return moderationItemRepository.findAllByOrderByDisplayOrderAsc()
        .stream()
        .map(this::toModerationItem)
        .toList();
  }

  @Transactional
  public MpesaPaymentResponse initiateMpesaPayment(String email, MpesaPaymentRequest request) {
    String checkoutRequestId = "ws_CO_" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
    String customerMessage = "STK push queued for " + request.phoneNumber()
        + ". Complete payment of KES " + request.amountKes() + ".";
    AppUser user = userRepository.findByEmailIgnoreCase(email).orElse(null);
    PaymentAttemptEntity attempt = paymentAttemptRepository.save(new PaymentAttemptEntity(
        checkoutRequestId,
        user,
        request.phoneNumber(),
        request.amountKes(),
        request.reference(),
        "QUEUED",
        customerMessage,
        Instant.now()
    ));
    return new MpesaPaymentResponse(
        attempt.getCheckoutRequestId(),
        attempt.getStatus(),
        attempt.getCustomerMessage()
    );
  }

  private AppUser requireUser(String email) {
    return userRepository.findByEmailIgnoreCase(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
  }

  private CourseEntity requireCourseEntity(String courseId) {
    return courseRepository.findWithTopicsById(courseId)
        .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));
  }

  private CourseEntity requireEnrolledCourseEntity(String email, String courseId) {
    List<CourseEntity> enrolledCourses = enrolledCourseEntities(email);
    return enrolledCourses.stream()
        .filter(course -> course.getId().equals(courseId))
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Course not enrolled: " + courseId));
  }

  private List<CourseEntity> allCourseEntities() {
    return courseRepository.findAllByOrderByDisplayOrderAsc();
  }

  private List<CourseEntity> enrolledCourseEntities(String email) {
    return enrolledCourseEntities(requireUser(email));
  }

  private List<CourseEntity> enrolledCourseEntities(AppUser user) {
    if (user.getRole() == UserRole.ADMIN) {
      return allCourseEntities();
    }
    List<CourseEntity> enrolledCourses = courseEnrollmentRepository
        .findByUserEmailIgnoreCaseOrderByCourseDisplayOrderAsc(user.getEmail())
        .stream()
        .map(CourseEnrollmentEntity::getCourse)
        .toList();
    return enrolledCourses.isEmpty() ? allCourseEntities() : enrolledCourses;
  }

  private Set<String> enrolledCourseNames(String email) {
    return enrolledCourseEntities(email)
        .stream()
        .map(CourseEntity::getCourseName)
        .collect(Collectors.toSet());
  }

  private CoursePrediction toCoursePrediction(CourseEntity course) {
    return new CoursePrediction(
        course.getId(),
        course.getCourseName(),
        course.getLecturer(),
        course.getCertainty(),
        course.getBaseScore(),
        mapTopics(course)
    );
  }

  private List<TopicPrediction> mapTopics(CourseEntity course) {
    return course.getTopics().stream()
        .sorted(Comparator.comparing(TopicPredictionEntity::getDisplayOrder))
        .map(topic -> new TopicPrediction(
            topic.getTopic(),
            topic.getLikelihood(),
            topic.getWeight(),
            topic.getRecommendedAction()
        ))
        .toList();
  }

  private StudyTask toStudyTask(StudyTaskEntity task) {
    return new StudyTask(
        task.getId(),
        task.getScheduledTime(),
        task.getTag(),
        task.getCourse(),
        task.getTitle(),
        task.getDescription(),
        task.getMinutes(),
        task.getAccent(),
        task.getPriority(),
        task.isCompleted()
    );
  }

  private Flashcard toFlashcard(FlashcardEntity card) {
    return new Flashcard(
        card.getId(),
        card.getCourse(),
        card.getQuestion(),
        card.getAnswer(),
        card.getDueInHours(),
        card.getMastery()
    );
  }

  private FeedSignal toFeedSignal(FeedSignalEntity signal) {
    return new FeedSignal(
        signal.getId(),
        signal.getIcon(),
        signal.getTitle(),
        signal.getSource(),
        signal.getBody(),
        signal.getCreatedAt(),
        signal.isVerified()
    );
  }

  private NotePack toNotePack(NotePackEntity notePack) {
    return new NotePack(
        notePack.getId(),
        notePack.getTitle(),
        notePack.getAuthor(),
        notePack.getPriceKes(),
        notePack.getRating(),
        notePack.getTag(),
        notePack.isVerified()
    );
  }

  private ModerationItem toModerationItem(ModerationItemEntity item) {
    return new ModerationItem(
        item.getId(),
        item.getItem(),
        item.getType(),
        item.getStatus(),
        item.getReason()
    );
  }

  private int certaintyFor(PredictionInput input, int baseCertainty) {
    int dataCompleteness = (input.revisionAverage() > 0 ? 5 : 0)
        + (input.attendance() > 0 ? 5 : 0)
        + (input.pastPaperCoverage() > 0 ? 5 : 0)
        + (input.assignmentScore() > 0 ? 5 : 0);
    return Math.min(96, baseCertainty + dataCompleteness / 2);
  }

  private String gradeFor(int score) {
    if (score >= 80) {
      return "A";
    }
    if (score >= 75) {
      return "A-";
    }
    if (score >= 70) {
      return "B+";
    }
    if (score >= 65) {
      return "B";
    }
    if (score >= 60) {
      return "B-";
    }
    if (score >= 55) {
      return "C+";
    }
    return "C";
  }

  private String riskFor(int score) {
    if (score >= 75) {
      return "LOW";
    }
    if (score >= 65) {
      return "MEDIUM";
    }
    return "HIGH";
  }
}
