package com.predicted.api.auth;

import com.predicted.api.common.ConflictException;
import com.predicted.api.persistence.AppUser;
import com.predicted.api.persistence.AppUserRepository;
import com.predicted.api.persistence.CourseEntity;
import com.predicted.api.persistence.CourseEnrollmentEntity;
import com.predicted.api.persistence.CourseEnrollmentRepository;
import com.predicted.api.persistence.CourseRepository;
import com.predicted.api.persistence.FlashcardEntity;
import com.predicted.api.persistence.FlashcardRepository;
import com.predicted.api.persistence.StudyTaskEntity;
import com.predicted.api.persistence.StudyTaskRepository;
import com.predicted.api.persistence.UserRole;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserRegistrationService {

  private final AppUserRepository userRepository;
  private final CourseRepository courseRepository;
  private final CourseEnrollmentRepository courseEnrollmentRepository;
  private final StudyTaskRepository studyTaskRepository;
  private final FlashcardRepository flashcardRepository;
  private final PasswordEncoder passwordEncoder;

  public UserRegistrationService(
      AppUserRepository userRepository,
      CourseRepository courseRepository,
      CourseEnrollmentRepository courseEnrollmentRepository,
      StudyTaskRepository studyTaskRepository,
      FlashcardRepository flashcardRepository,
      PasswordEncoder passwordEncoder
  ) {
    this.userRepository = userRepository;
    this.courseRepository = courseRepository;
    this.courseEnrollmentRepository = courseEnrollmentRepository;
    this.studyTaskRepository = studyTaskRepository;
    this.flashcardRepository = flashcardRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public AppUser register(RegisterRequest request) {
    String email = request.email().trim().toLowerCase(Locale.ROOT);
    if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
      throw new ConflictException("An account already exists for " + email + ".");
    }

    AppUser user = new AppUser(
        "usr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
        request.name().trim(),
        email,
        passwordEncoder.encode(request.password()),
        request.university().trim(),
        request.program().trim(),
        request.academicLevel().trim(),
        UserRole.STUDENT
    );

    try {
      AppUser saved = userRepository.saveAndFlush(user);
      List<String> courseIds = selectedCourseIds(request.courseIds());
      enroll(saved, courseIds);
      seedStarterTasks(saved, courseIds);
      seedStarterFlashcards(saved, courseIds);
      return saved;
    } catch (DataIntegrityViolationException exception) {
      throw new ConflictException("An account already exists for " + email + ".");
    }
  }

  private List<String> selectedCourseIds(List<String> requestedCourseIds) {
    List<String> ids = requestedCourseIds == null || requestedCourseIds.isEmpty()
        ? List.of("distributed", "ai", "compiler")
        : requestedCourseIds;
    List<String> uniqueIds = ids.stream()
        .map(id -> id.trim().toLowerCase(Locale.ROOT))
        .filter(id -> !id.isBlank())
        .collect(Collectors.toCollection(LinkedHashSet::new))
        .stream()
        .toList();
    if (uniqueIds.isEmpty()) {
      return List.of("distributed", "ai", "compiler");
    }
    List<String> knownIds = courseRepository.findAllById(uniqueIds)
        .stream()
        .map(CourseEntity::getId)
        .toList();
    if (knownIds.size() != uniqueIds.size()) {
      throw new ConflictException("One or more selected courses are not available.");
    }
    return uniqueIds;
  }

  private void enroll(AppUser user, List<String> courseIds) {
    List<CourseEntity> courses = courseRepository.findAllById(courseIds);
    Map<String, CourseEntity> byId = courses.stream()
        .collect(java.util.stream.Collectors.toMap(CourseEntity::getId, course -> course));
    courseIds.forEach(courseId -> courseEnrollmentRepository.save(
        new CourseEnrollmentEntity(user, byId.get(courseId), Instant.now())
    ));
  }

  private void seedStarterTasks(AppUser user, List<String> courseIds) {
    String prefix = user.getId();
    List<StudyTaskEntity> tasks = new ArrayList<>();
    for (String courseId : courseIds) {
      switch (courseId) {
        case "distributed" -> tasks.add(new StudyTaskEntity(prefix + "_task_vectors", user, LocalTime.of(14, 0),
            "High Yield", "Distributed Systems", "Vector clocks sprint",
            "Practice vector clocks with 3-process timelines.", 35, "#cf3f4f", "HIGH"));
        case "ai" -> tasks.add(new StudyTaskEntity(prefix + "_task_bayes", user, LocalTime.of(16, 30),
            "Revision", "Artificial Intelligence", "Bayesian networks", "Drill independence and d-separation.",
            45, "#1d4ed8", "HIGH"));
        case "compiler" -> tasks.add(new StudyTaskEntity(prefix + "_task_ll1", user, LocalTime.of(19, 0),
            "Lab Prep", "Compiler Construction", "LL(1) parsing table",
            "Build a parsing table from grammar exercises.", 55, "#0f9fbc", "MEDIUM"));
        case "data" -> tasks.add(new StudyTaskEntity(prefix + "_task_rules", user, LocalTime.of(21, 0),
            "Flashcards", "Data Mining", "Association rules", "Review confidence, support, and lift.", 25,
            "#c97800", "MEDIUM"));
        default -> {
        }
      }
    }
    studyTaskRepository.saveAll(tasks.stream().limit(3).toList());
  }

  private void seedStarterFlashcards(AppUser user, List<String> courseIds) {
    String prefix = user.getId();
    List<FlashcardEntity> cards = new ArrayList<>();
    for (String courseId : courseIds) {
      switch (courseId) {
        case "distributed" -> cards.add(new FlashcardEntity(prefix + "_card_vectors", user, "Distributed Systems",
            "What problem do vector clocks solve?", "They infer causal ordering without a global clock.", 0, 40));
        case "ai" -> cards.add(new FlashcardEntity(prefix + "_card_bayes", user, "Artificial Intelligence",
            "What does d-separation test?",
            "It tests conditional independence by checking whether evidence blocks graph paths.", 0, 35));
        case "compiler" -> cards.add(new FlashcardEntity(prefix + "_card_ll1", user, "Compiler Construction",
            "What makes a grammar LL(1)?", "One lookahead token chooses exactly one production per nonterminal.", 2,
            30));
        case "data" -> cards.add(new FlashcardEntity(prefix + "_card_lift", user, "Data Mining",
            "What does lift measure?", "Observed co-occurrence compared with expected co-occurrence.", 3, 35));
        default -> {
        }
      }
    }
    flashcardRepository.saveAll(cards.stream().limit(3).toList());
  }
}
