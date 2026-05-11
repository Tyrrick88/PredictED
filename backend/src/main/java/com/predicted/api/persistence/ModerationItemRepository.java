package com.predicted.api.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModerationItemRepository extends JpaRepository<ModerationItemEntity, String> {

  List<ModerationItemEntity> findAllByOrderByDisplayOrderAsc();
}
