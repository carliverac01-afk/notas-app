package com.example.notes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.example.notes.model.Note;

import java.time.Instant;

public final class NoteDtos {
    private NoteDtos() {
    }

    public record CreateNoteRequest(
            @NotBlank @Size(max = 160) String title,
            @NotBlank String content
    ) {
    }

    public record UpdateNoteRequest(
            @NotBlank @Size(max = 160) String title,
            @NotBlank String content
    ) {
    }

    public record NoteResponse(
            Long id,
            String title,
            String content,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        public static NoteResponse from(Note note) {
            return new NoteResponse(
                    note.getId(),
                    note.getTitle(),
                    note.getContent(),
                    note.getCreatedAt(),
                    note.getUpdatedAt(),
                    note.getDeletedAt()
            );
        }
    }
}
