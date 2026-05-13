package com.predicted.api.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcademicModuleRepository extends JpaRepository<AcademicModuleEntity, Long> {

  List<AcademicModuleEntity> findByPathIdOrderByDisplayOrderAsc(String pathId);
}
