package com.predicted.api.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlannerMilestoneRepository extends JpaRepository<PlannerMilestoneEntity, Long> {

  List<PlannerMilestoneEntity> findByUserIdOrderByDueAtAsc(String userId);

  void deleteByUserId(String userId);
}
