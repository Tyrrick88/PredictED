package com.predicted.api.common;

import com.predicted.api.auth.AuthController;
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
import com.predicted.api.common.Models.StudyTask;
import com.predicted.api.common.Models.TopicPrediction;
import com.predicted.api.common.Models.TutorRequest;
import com.predicted.api.common.Models.TutorResponse;
import com.predicted.api.common.Models.UserProfile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class AcademicDataService {

  private final Map<String, CoursePrediction> courses;
  private final List<StudyTask> baseTasks;
  private final CopyOnWriteArrayList<FeedSignal> feedSignals;
  private final List<Flashcard> flashcards;
  private final List<NotePack> notePacks;
  private final List<ModerationItem> moderationItems;
  private final Set<String> completedTasks = ConcurrentHashMap.newKeySet();
  private final Map<String, Integer> cardMastery = new ConcurrentHashMap<>();

  public AcademicDataService() {
    this.courses = seedCourses();
    this.baseTasks = seedTasks();
    this.feedSignals = new CopyOnWriteArrayList<>(seedFeed());
    this.flashcards = seedFlashcards();
    this.notePacks = seedNotePacks();
    this.moderationItems = seedModeration();
  }

  public DashboardOverview dashboard(String email) {
    CoursePrediction distributedSystems = requireCourse("distributed");
    PredictionSummary prediction = new PredictionSummary(
        distributedSystems.baseScore(),
        gradeFor(distributedSystems.baseScore()),
        distributedSystems.certainty(),
        riskFor(distributedSystems.baseScore()),
        0.20,
        distributedSystems.topics().stream().limit(3).map(TopicPrediction::topic).toList()
    );

    return new DashboardOverview(
        AuthController.profileFor(email),
        prediction,
        new GamificationSummary(24, "Senior Scholar", 12450, 15000, 14, 128.5),
        planner(3).tasks(),
        feed(),
        flashcards.size(),
        4
    );
  }

  public List<CoursePrediction> courses() {
    return List.copyOf(courses.values());
  }

  public CoursePrediction requireCourse(String courseId) {
    CoursePrediction course = courses.get(courseId);
    if (course == null) {
      throw new ResourceNotFoundException("Course not found: " + courseId);
    }
    return course;
  }

  public PredictionResult simulate(String courseId, PredictionInput input) {
    CoursePrediction course = requireCourse(courseId);
    int score = Math.round(
        (float) ((input.revisionAverage() * 0.36)
            + (input.attendance() * 0.16)
            + (input.pastPaperCoverage() * 0.28)
            + (input.assignmentScore() * 0.20))
    );
    double expectedGpaLift = Math.round(((score - course.baseScore()) / 100.0) * 100.0) / 100.0;

    return new PredictionResult(
        course.courseId(),
        score,
        gradeFor(score),
        riskFor(score),
        certaintyFor(input, course.certainty()),
        expectedGpaLift,
        course.topics().stream()
            .sorted(Comparator.comparing(TopicPrediction::likelihood).reversed())
            .limit(3)
            .toList()
    );
  }

  public List<MockQuestion> generateMock(String courseId) {
    CoursePrediction course = requireCourse(courseId);
    List<MockQuestion> questions = new ArrayList<>();
    for (int index = 0; index < Math.min(3, course.topics().size()); index += 1) {
      TopicPrediction topic = course.topics().get(index);
      String prompt = switch (index) {
        case 0 -> "Explain " + topic.topic() + " using a lecturer-style worked example.";
        case 1 -> "Compare two approaches related to " + topic.topic() + " and justify the stronger answer.";
        default -> "Solve a short scenario involving " + topic.topic() + " and show each step.";
      };
      questions.add(new MockQuestion(
          index + 1,
          course.courseId(),
          topic.topic(),
          prompt,
          index == 0 ? 12 : 8,
          topic.recommendedAction()
      ));
    }
    return questions;
  }

  public PlannerResponse planner(int focusHours) {
    int boundedHours = Math.max(1, Math.min(5, focusHours));
    int visibleTasks = Math.min(baseTasks.size(), Math.max(2, boundedHours + 1));
    List<StudyTask> tasks = baseTasks.stream()
        .limit(visibleTasks)
        .map(task -> taskWithCompletion(task, completedTasks.contains(task.id())))
        .toList();
    int totalMinutes = tasks.stream().mapToInt(StudyTask::minutes).sum();
    return new PlannerResponse(boundedHours, totalMinutes, tasks);
  }

  public StudyTask completeTask(String taskId) {
    StudyTask task = baseTasks.stream()
        .filter(candidate -> candidate.id().equals(taskId))
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Study task not found: " + taskId));
    completedTasks.add(taskId);
    return taskWithCompletion(task, true);
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

  public List<Flashcard> dueFlashcards() {
    return flashcards.stream()
        .map(card -> new Flashcard(
            card.id(),
            card.course(),
            card.question(),
            card.answer(),
            card.dueInHours(),
            cardMastery.getOrDefault(card.id(), card.mastery())
        ))
        .toList();
  }

  public FlashcardReviewResponse reviewFlashcard(String cardId, FlashcardRating rating) {
    Flashcard card = flashcards.stream()
        .filter(candidate -> candidate.id().equals(cardId))
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Flashcard not found: " + cardId));
    int current = cardMastery.getOrDefault(card.id(), card.mastery());
    int updated = switch (rating) {
      case HARD -> Math.max(0, current - 5);
      case GOOD -> Math.min(100, current + 8);
      case EASY -> Math.min(100, current + 14);
    };
    int nextReviewHours = switch (rating) {
      case HARD -> 4;
      case GOOD -> 24;
      case EASY -> 72;
    };
    cardMastery.put(card.id(), updated);
    return new FlashcardReviewResponse(card.id(), rating, nextReviewHours, updated);
  }

  public List<FeedSignal> feed() {
    return feedSignals.stream()
        .sorted(Comparator.comparing(FeedSignal::createdAt).reversed())
        .toList();
  }

  public FeedSignal createFeedSignal(CreateFeedSignalRequest request) {
    FeedSignal signal = new FeedSignal(
        "sig_" + UUID.randomUUID().toString().substring(0, 8),
        "bolt",
        request.title(),
        request.source() == null || request.source().isBlank() ? "Student Signal" : request.source(),
        request.body(),
        Instant.now(),
        false
    );
    feedSignals.add(signal);
    return signal;
  }

  public List<NotePack> notePacks() {
    return notePacks;
  }

  public List<ModerationItem> moderationItems() {
    return moderationItems;
  }

  public MpesaPaymentResponse initiateMpesaPayment(MpesaPaymentRequest request) {
    return new MpesaPaymentResponse(
        "ws_CO_" + UUID.randomUUID().toString().replace("-", "").substring(0, 18),
        "QUEUED",
        "STK push queued for " + request.phoneNumber() + ". Complete payment of KES " + request.amountKes() + "."
    );
  }

  private Map<String, CoursePrediction> seedCourses() {
    Map<String, CoursePrediction> seeded = new LinkedHashMap<>();
    seeded.put("distributed", new CoursePrediction(
        "distributed",
        "Distributed Systems",
        "Dr. Njuguna",
        89,
        74,
        List.of(
            new TopicPrediction("Vector clocks", 91, "High", "Show update, send, receive, and comparison rules."),
            new TopicPrediction("Lamport timestamps", 88, "High", "Explain ordering limits and tie-breaking."),
            new TopicPrediction("Mutual exclusion", 76, "Medium", "Compare token and permission approaches."),
            new TopicPrediction("Replication consistency", 69, "Medium", "Use a real data-replica scenario.")
        )
    ));
    seeded.put("ai", new CoursePrediction(
        "ai",
        "Artificial Intelligence",
        "Prof. Otieno",
        84,
        72,
        List.of(
            new TopicPrediction("Bayesian networks", 90, "High", "State graph assumptions before factorization."),
            new TopicPrediction("A* search", 82, "High", "Mention admissibility and consistency."),
            new TopicPrediction("Constraint satisfaction", 74, "Medium", "Show variable, domain, and constraint setup."),
            new TopicPrediction("Minimax pruning", 66, "Medium", "Trace alpha and beta updates.")
        )
    ));
    seeded.put("compiler", new CoursePrediction(
        "compiler",
        "Compiler Construction",
        "Dr. Karanja",
        86,
        68,
        List.of(
            new TopicPrediction("FIRST/FOLLOW sets", 93, "High", "Write epsilon cases carefully."),
            new TopicPrediction("LL(1) parsing table", 86, "High", "Check conflicts after filling every cell."),
            new TopicPrediction("Lexical analysis", 72, "Medium", "Differentiate tokens, patterns, and lexemes."),
            new TopicPrediction("Intermediate code", 61, "Medium", "Use triples or quadruples consistently.")
        )
    ));
    seeded.put("data", new CoursePrediction(
        "data",
        "Data Mining",
        "Dr. Achieng",
        81,
        76,
        List.of(
            new TopicPrediction("Association rules", 87, "High", "Compute support, confidence, and lift."),
            new TopicPrediction("Decision trees", 82, "High", "Show information-gain calculations."),
            new TopicPrediction("Clustering metrics", 78, "Medium", "Name the metric before interpreting it."),
            new TopicPrediction("Naive Bayes", 70, "Medium", "State independence assumptions.")
        )
    ));
    return seeded;
  }

  private List<StudyTask> seedTasks() {
    return List.of(
        new StudyTask("task_ds_vectors", LocalTime.of(14, 0), "High Yield", "Distributed Systems",
            "Vector clocks sprint", "Practice vector clocks with 3-process timelines.", 35, "#cf3f4f", "HIGH", false),
        new StudyTask("task_ai_bayes", LocalTime.of(16, 30), "Revision", "Artificial Intelligence",
            "Bayesian networks", "Drill independence and d-separation.", 45, "#1d4ed8", "HIGH", false),
        new StudyTask("task_compiler_ll1", LocalTime.of(19, 0), "Lab Prep", "Compiler Construction",
            "LL(1) parsing table", "Build a parsing table from grammar exercises.", 55, "#0f9fbc", "MEDIUM", false),
        new StudyTask("task_data_rules", LocalTime.of(21, 0), "Flashcards", "Data Mining",
            "Association rules", "Review confidence, support, and lift.", 25, "#c97800", "MEDIUM", false)
    );
  }

  private List<FeedSignal> seedFeed() {
    Instant now = Instant.now();
    return List.of(
        new FeedSignal("sig_cat_moved", "campaign", "AI CAT moved to Thursday", "Class rep",
            "Venue changed to LT2. Lecturer confirmed question scope stays the same.", now.minusSeconds(18 * 60), true),
        new FeedSignal("sig_data_pack", "description", "Data Mining past paper uploaded", "Marketplace",
            "Includes 2023 marking guide and lecturer annotations.", now.minusSeconds(42 * 60), true),
        new FeedSignal("sig_compiler_room", "groups", "Compiler lab sprint opened", "Study Group",
            "8 students joined the FIRST/FOLLOW debugging room.", now.minusSeconds(60 * 60), false),
        new FeedSignal("sig_ds_pattern", "verified", "Distributed Systems pattern verified", "Predictive Engine",
            "Vector-clock questions appeared in 4 of 5 recent exams.", now.minusSeconds(2 * 60 * 60), true)
    );
  }

  private List<Flashcard> seedFlashcards() {
    return List.of(
        new Flashcard("card_vectors", "Distributed Systems", "What problem do vector clocks solve?",
            "They infer causal ordering without a global clock.", 0, 62),
        new Flashcard("card_bayes", "Artificial Intelligence", "What does d-separation test?",
            "It tests conditional independence by checking whether evidence blocks graph paths.", 0, 58),
        new Flashcard("card_ll1", "Compiler Construction", "What makes a grammar LL(1)?",
            "One lookahead token chooses exactly one production per nonterminal.", 2, 54),
        new Flashcard("card_lift", "Data Mining", "What does lift measure?",
            "Observed co-occurrence compared with expected co-occurrence.", 3, 68)
    );
  }

  private List<NotePack> seedNotePacks() {
    return List.of(
        new NotePack("pack_ds_final", "Distributed Systems Final Pack", "Brian O.", 80, 4.9, "Verified", true),
        new NotePack("pack_ai_cat", "AI CAT Revision Notes", "Mary W.", 45, 4.7, "Hot", true),
        new NotePack("pack_compiler_labs", "Compiler Lab Walkthroughs", "CS Club", 120, 4.8, "Bundle", true),
        new NotePack("pack_data_papers", "Data Mining Past Papers", "Amina K.", 60, 4.6, "New", false)
    );
  }

  private List<ModerationItem> seedModeration() {
    return List.of(
        new ModerationItem("mod_ds_pack", "Distributed Systems Final Pack", "Notes", "Review", "Needs source confirmation"),
        new ModerationItem("mod_cat_moved", "CAT moved to Thursday", "Feed", "Approved", "Verified by class rep"),
        new ModerationItem("mod_compiler_pack", "Compiler Lab Walkthroughs", "Marketplace", "Review", "Check duplicate upload"),
        new ModerationItem("mod_ds_group", "DS Past Paper Sprint", "Group", "Approved", "Community guidelines passed")
    );
  }

  private StudyTask taskWithCompletion(StudyTask task, boolean completed) {
    return new StudyTask(
        task.id(),
        task.time(),
        task.tag(),
        task.course(),
        task.title(),
        task.description(),
        task.minutes(),
        task.accent(),
        task.priority(),
        completed
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
