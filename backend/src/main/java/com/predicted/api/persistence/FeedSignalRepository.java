package com.predicted.api.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedSignalRepository extends JpaRepository<FeedSignalEntity, String> {

  List<FeedSignalEntity> findAllByOrderByCreatedAtDesc();

  List<FeedSignalEntity> findTop4ByOrderByCreatedAtDesc();
}
