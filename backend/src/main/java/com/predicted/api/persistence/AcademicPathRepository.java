package com.predicted.api.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AcademicPathRepository extends JpaRepository<AcademicPathEntity, String> {

  @EntityGraph(attributePaths = {"modules", "resources", "resources.module"})
  List<AcademicPathEntity> findAllByOrderByDisplayOrderAsc();

  @EntityGraph(attributePaths = {"modules", "resources", "resources.module"})
  Optional<AcademicPathEntity> findById(String id);
}
