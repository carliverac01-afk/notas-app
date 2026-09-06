package com.example.notes.controller;

import com.example.notes.security.AuthenticatedUser;
import com.example.notes.service.NoteService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

import static com.example.notes.dto.NoteDtos.CreateNoteRequest;
import static com.example.notes.dto.NoteDtos.NoteResponse;
import static com.example.notes.dto.NoteDtos.UpdateNoteRequest;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping
    public List<NoteResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return noteService.listActive(user.id());
    }

    @GetMapping("/sync")
    public List<NoteResponse> sync(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant updatedAfter
    ) {
        return noteService.sync(user.id(), updatedAfter);
    }

    @GetMapping("/{id}")
    public NoteResponse getById(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id
    ) {
        return noteService.getById(user.id(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NoteResponse create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateNoteRequest request
    ) {
        return noteService.create(user.id(), request);
    }

    @PutMapping("/{id}")
    public NoteResponse update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id,
            @Valid @RequestBody UpdateNoteRequest request
    ) {
        return noteService.update(user.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id
    ) {
        noteService.delete(user.id(), id);
    }
}
