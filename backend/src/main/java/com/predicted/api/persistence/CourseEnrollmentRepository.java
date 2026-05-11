package com.predicted.api.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollmentEntity, Long> {

  List<CourseEnrollmentEntity> findByUserEmailIgnoreCaseOrderByCourseDisplayOrderAsc(String email);

  long countByUserEmailIgnoreCase(String email);

  void deleteByUser(AppUser user);
}
