package com.predicted.api.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcademicResourceRepository extends JpaRepository<AcademicResourceEntity, Long> {

  List<AcademicResourceEntity> findByPathIdOrderByDisplayOrderAsc(String pathId);

  List<AcademicResourceEntity> findByModuleIdOrderByDisplayOrderAsc(Long moduleId);
}
