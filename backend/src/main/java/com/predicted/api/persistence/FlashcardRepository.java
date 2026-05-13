package com.predicted.api.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FlashcardRepository extends JpaRepository<FlashcardEntity, String> {

  List<FlashcardEntity> findByUserEmailIgnoreCaseOrderByDueInHoursAscIdAsc(String email);

  List<FlashcardEntity> findByUserEmailIgnoreCaseAndCourseInOrderByDueInHoursAscIdAsc(
      String email,
      Collection<String> courses
  );

  long countByUserEmailIgnoreCase(String email);

  long countByUserEmailIgnoreCaseAndCourseIn(String email, Collection<String> courses);

  Optional<FlashcardEntity> findByIdAndUserEmailIgnoreCase(String id, String email);
}
