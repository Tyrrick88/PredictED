package com.predicted.api.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FlashcardRepository extends JpaRepository<FlashcardEntity, String> {

  List<FlashcardEntity> findByUserEmailIgnoreCaseOrderByDueInHoursAscIdAsc(String email);

  Optional<FlashcardEntity> findByIdAndUserEmailIgnoreCase(String id, String email);
}
