package com.example.notes.service;

import com.example.notes.model.AppUser;
import com.example.notes.model.Note;
import com.example.notes.repository.NoteRepository;
import com.example.notes.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static com.example.notes.dto.NoteDtos.CreateNoteRequest;
import static com.example.notes.dto.NoteDtos.NoteResponse;
import static com.example.notes.dto.NoteDtos.UpdateNoteRequest;

@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    public NoteService(NoteRepository noteRepository, UserRepository userRepository) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> listActive(Long userId) {
        return noteRepository.findByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(userId)
                .stream()
                .map(NoteResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> sync(Long userId, Instant updatedAfter) {
        return noteRepository.findByUserIdAndUpdatedAtAfterOrderByUpdatedAtAsc(userId, updatedAfter)
                .stream()
                .map(NoteResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public NoteResponse getById(Long userId, Long noteId) {
        Note note = findOwnedNote(userId, noteId);
        if (note.isDeleted()) {
            throw new EntityNotFoundException("Nota no encontrada");
        }
        return NoteResponse.from(note);
    }

    @Transactional
    public NoteResponse create(Long userId, CreateNoteRequest request) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        Note note = noteRepository.save(new Note(user, request.title().trim(), request.content().trim()));
        return NoteResponse.from(note);
    }

    @Transactional
    public NoteResponse update(Long userId, Long noteId, UpdateNoteRequest request) {
        Note note = findOwnedNote(userId, noteId);
        if (note.isDeleted()) {
            throw new EntityNotFoundException("Nota no encontrada");
        }

        note.update(request.title().trim(), request.content().trim());
        return NoteResponse.from(note);
    }

    @Transactional
    public void delete(Long userId, Long noteId) {
        Note note = findOwnedNote(userId, noteId);
        if (!note.isDeleted()) {
            note.softDelete();
        }
    }

    private Note findOwnedNote(Long userId, Long noteId) {
        return noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Nota no encontrada"));
    }
}
