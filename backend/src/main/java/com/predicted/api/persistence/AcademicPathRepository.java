package com.predicted.api.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AcademicPathRepository extends JpaRepository<AcademicPathEntity, String> {

  List<AcademicPathEntity> findAllByOrderByDisplayOrderAsc();

  Optional<AcademicPathEntity> findById(String id);
}
