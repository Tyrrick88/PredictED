package com.predicted.api.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PlannerSessionRepository extends JpaRepository<PlannerSessionEntity, String> {

  List<PlannerSessionEntity> findByUserIdOrderBySessionDateAscScheduledTimeAsc(String userId);

  List<PlannerSessionEntity> findByUserIdAndSessionDateBetweenOrderBySessionDateAscScheduledTimeAsc(
      String userId,
      LocalDate from,
      LocalDate to
  );

  Optional<PlannerSessionEntity> findByIdAndUserId(String id, String userId);

  void deleteByUserIdAndSessionDateGreaterThanEqualAndCompletedFalse(String userId, LocalDate fromDate);
}
