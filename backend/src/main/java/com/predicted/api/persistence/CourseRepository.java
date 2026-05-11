package com.predicted.api.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<CourseEntity, String> {

  @EntityGraph(attributePaths = "topics")
  List<CourseEntity> findAllByOrderByDisplayOrderAsc();

  @Query("select c from CourseEntity c left join fetch c.topics where c.id = :id")
  Optional<CourseEntity> findWithTopicsById(@Param("id") String id);
}
