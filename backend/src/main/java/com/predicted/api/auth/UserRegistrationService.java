package com.predicted.api.auth;

import com.predicted.api.common.ConflictException;
import com.predicted.api.persistence.AppUser;
import com.predicted.api.persistence.AppUserRepository;
import com.predicted.api.persistence.FlashcardEntity;
import com.predicted.api.persistence.FlashcardRepository;
import com.predicted.api.persistence.StudyTaskEntity;
import com.predicted.api.persistence.StudyTaskRepository;
import com.predicted.api.persistence.UserRole;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class UserRegistrationService {

  private final AppUserRepository userRepository;
  private final StudyTaskRepository studyTaskRepository;
  private final FlashcardRepository flashcardRepository;
  private final PasswordEncoder passwordEncoder;

  public UserRegistrationService(
      AppUserRepository userRepository,
      StudyTaskRepository studyTaskRepository,
      FlashcardRepository flashcardRepository,
      PasswordEncoder passwordEncoder
  ) {
    this.userRepository = userRepository;
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
      seedStarterTasks(saved);
      seedStarterFlashcards(saved);
      return saved;
    } catch (DataIntegrityViolationException exception) {
      throw new ConflictException("An account already exists for " + email + ".");
    }
  }

  private void seedStarterTasks(AppUser user) {
    String prefix = user.getId();
    studyTaskRepository.save(new StudyTaskEntity(prefix + "_task_vectors", user, LocalTime.of(14, 0), "High Yield",
        "Distributed Systems", "Vector clocks sprint", "Practice vector clocks with 3-process timelines.", 35,
        "#cf3f4f", "HIGH"));
    studyTaskRepository.save(new StudyTaskEntity(prefix + "_task_bayes", user, LocalTime.of(16, 30), "Revision",
        "Artificial Intelligence", "Bayesian networks", "Drill independence and d-separation.", 45,
        "#1d4ed8", "HIGH"));
    studyTaskRepository.save(new StudyTaskEntity(prefix + "_task_ll1", user, LocalTime.of(19, 0), "Lab Prep",
        "Compiler Construction", "LL(1) parsing table", "Build a parsing table from grammar exercises.", 55,
        "#0f9fbc", "MEDIUM"));
  }

  private void seedStarterFlashcards(AppUser user) {
    String prefix = user.getId();
    flashcardRepository.save(new FlashcardEntity(prefix + "_card_vectors", user, "Distributed Systems",
        "What problem do vector clocks solve?", "They infer causal ordering without a global clock.", 0, 40));
    flashcardRepository.save(new FlashcardEntity(prefix + "_card_bayes", user, "Artificial Intelligence",
        "What does d-separation test?",
        "It tests conditional independence by checking whether evidence blocks graph paths.", 0, 35));
    flashcardRepository.save(new FlashcardEntity(prefix + "_card_ll1", user, "Compiler Construction",
        "What makes a grammar LL(1)?", "One lookahead token chooses exactly one production per nonterminal.", 2, 30));
  }
}
