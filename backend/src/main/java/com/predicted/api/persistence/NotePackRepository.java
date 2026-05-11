package com.predicted.api.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotePackRepository extends JpaRepository<NotePackEntity, String> {

  List<NotePackEntity> findAllByOrderByDisplayOrderAsc();
}
