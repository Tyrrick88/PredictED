package com.predicted.api.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyTaskRepository extends JpaRepository<StudyTaskEntity, String> {

  List<StudyTaskEntity> findByUserEmailIgnoreCaseOrderByScheduledTimeAsc(String email);

  Optional<StudyTaskEntity> findByIdAndUserEmailIgnoreCase(String id, String email);
}
