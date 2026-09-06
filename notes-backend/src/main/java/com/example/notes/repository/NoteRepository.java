package com.example.notes.repository;

import com.example.notes.model.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(Long userId);

    List<Note> findByUserIdAndUpdatedAtAfterOrderByUpdatedAtAsc(Long userId, Instant updatedAfter);

    Optional<Note> findByIdAndUserId(Long id, Long userId);
}
