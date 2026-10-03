package com.example.notesandroid.repository;

import android.content.Context;

import com.example.notesandroid.api.ApiClient;
import com.example.notesandroid.api.ApiErrorResponse;
import com.example.notesandroid.api.AuthRequest;
import com.example.notesandroid.api.AuthResponse;
import com.example.notesandroid.api.NoteRequest;
import com.example.notesandroid.api.NoteResponse;
import com.example.notesandroid.api.NotesApi;
import com.example.notesandroid.api.RegisterRequest;
import com.example.notesandroid.data.AppDatabase;
import com.example.notesandroid.data.NoteDao;
import com.example.notesandroid.data.NoteEntity;
import com.google.gson.Gson;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import retrofit2.Response;

public class NotesRepository {
    private static final String ACTION_CREATE = "CREATE";
    private static final String ACTION_UPDATE = "UPDATE";
    private static final String ACTION_DELETE = "DELETE";

    private final Context context;
    private final NotesApi api;
    private final NoteDao noteDao;
    private final SessionManager session;
    private final Gson gson = new Gson();

    public NotesRepository(Context context) {
        this.context = context.getApplicationContext();
        this.api = ApiClient.getNotesApi();
        this.noteDao = AppDatabase.getInstance(context).noteDao();
        this.session = new SessionManager(context);
    }

    public boolean isLoggedIn() {
        return session.isLoggedIn();
    }

    public UserSession getUserSession() {
        return session.getUserSession();
    }

    public void logout() {
        session.logout();
    }

    public void login(String email, String password) throws IOException {
        requireInternetForAuth();
        Response<AuthResponse> response = api.login(new AuthRequest(email, password)).execute();
        saveTokenOrThrow(response, "No se pudo iniciar sesion");
    }

    public void register(String name, String email, String password) throws IOException {
        requireInternetForAuth();
        Response<AuthResponse> response = api.register(new RegisterRequest(email, name, password)).execute();
        saveTokenOrThrow(response, "No se pudo crear el perfil");
    }

    public List<NoteEntity> getNotes() throws IOException {
        if (NetworkUtils.hasInternet(context) && session.isLoggedIn()) {
            sync();
        }
        return noteDao.getActiveNotes();
    }

    public void createNote(String title, String content) throws IOException {
        if (NetworkUtils.hasInternet(context) && session.isLoggedIn()) {
            Response<NoteResponse> response = api.createNote(
                    session.getAuthorizationHeader(),
                    new NoteRequest(title, content)
            ).execute();
            if (response.isSuccessful() && response.body() != null) {
                saveRemoteNote(response.body(), false);
                return;
            } else if (!response.isSuccessful()) {
                throw new IOException(apiErrorMessage(response, "No se pudo crear la nota"));
            }
        }

        NoteEntity note = new NoteEntity();
        note.title = title;
        note.content = content;
        note.createdAt = Instant.now().toString();
        note.updatedAt = note.createdAt;
        note.pendingSync = true;
        note.pendingAction = ACTION_CREATE;
        noteDao.insert(note);
    }

    public void updateNote(NoteEntity note, String title, String content) throws IOException {
        note.title = title;
        note.content = content;
        note.updatedAt = Instant.now().toString();

        if (NetworkUtils.hasInternet(context) && session.isLoggedIn() && note.remoteId != null) {
            Response<NoteResponse> response = api.updateNote(
                    session.getAuthorizationHeader(),
                    note.remoteId,
                    new NoteRequest(title, content)
            ).execute();
            if (response.isSuccessful() && response.body() != null) {
                saveRemoteNote(response.body(), false);
                return;
            } else if (!response.isSuccessful()) {
                throw new IOException(apiErrorMessage(response, "No se pudo actualizar la nota"));
            }
        }

        note.pendingSync = true;
        note.pendingAction = note.remoteId == null ? ACTION_CREATE : ACTION_UPDATE;
        noteDao.update(note);
    }

    public void deleteNote(NoteEntity note) throws IOException {
        if (NetworkUtils.hasInternet(context) && session.isLoggedIn() && note.remoteId != null) {
            Response<Void> response = api.deleteNote(session.getAuthorizationHeader(), note.remoteId).execute();
            if (response.isSuccessful()) {
                noteDao.delete(note);
                return;
            } else {
                throw new IOException(apiErrorMessage(response, "No se pudo eliminar la nota"));
            }
        }

        if (note.remoteId == null) {
            noteDao.delete(note);
            return;
        }

        note.deletedAt = Instant.now().toString();
        note.updatedAt = note.deletedAt;
        note.pendingSync = true;
        note.pendingAction = ACTION_DELETE;
        noteDao.update(note);
    }

    public void sync() throws IOException {
        uploadPendingChanges();

        Response<List<NoteResponse>> response = api.syncNotes(
                session.getAuthorizationHeader(),
                session.getLastSync()
        ).execute();

        if (!response.isSuccessful() || response.body() == null) {
            throw new IOException(apiErrorMessage(response, "No se pudo sincronizar con el servidor"));
        }

        String newestDate = session.getLastSync();
        for (NoteResponse remoteNote : response.body()) {
            saveRemoteNote(remoteNote, true);
            if (remoteNote.updatedAt != null && remoteNote.updatedAt.compareTo(newestDate) > 0) {
                newestDate = remoteNote.updatedAt;
            }
        }
        session.saveLastSync(newestDate);
    }

    private void uploadPendingChanges() throws IOException {
        for (NoteEntity note : noteDao.getPendingNotes()) {
            if (ACTION_CREATE.equals(note.pendingAction)) {
                Response<NoteResponse> response = api.createNote(
                        session.getAuthorizationHeader(),
                        new NoteRequest(note.title, note.content)
                ).execute();
                if (response.isSuccessful() && response.body() != null) {
                    noteDao.delete(note);
                    saveRemoteNote(response.body(), false);
                }
            } else if (ACTION_UPDATE.equals(note.pendingAction) && note.remoteId != null) {
                Response<NoteResponse> response = api.updateNote(
                        session.getAuthorizationHeader(),
                        note.remoteId,
                        new NoteRequest(note.title, note.content)
                ).execute();
                if (response.isSuccessful() && response.body() != null) {
                    saveRemoteNote(response.body(), false);
                }
            } else if (ACTION_DELETE.equals(note.pendingAction) && note.remoteId != null) {
                Response<Void> response = api.deleteNote(session.getAuthorizationHeader(), note.remoteId).execute();
                if (response.isSuccessful()) {
                    noteDao.delete(note);
                }
            }
        }
    }

    private void saveRemoteNote(NoteResponse remoteNote, boolean removeDeleted) {
        NoteEntity local = noteDao.findByRemoteId(remoteNote.id);
        if (local == null) {
            local = new NoteEntity();
        }

        local.remoteId = remoteNote.id;
        local.title = remoteNote.title;
        local.content = remoteNote.content;
        local.createdAt = remoteNote.createdAt;
        local.updatedAt = remoteNote.updatedAt;
        local.deletedAt = remoteNote.deletedAt;
        local.pendingSync = false;
        local.pendingAction = null;

        if (removeDeleted && local.isDeleted()) {
            if (local.localId != 0) {
                noteDao.delete(local);
            }
        } else if (local.localId == 0) {
            noteDao.insert(local);
        } else {
            noteDao.update(local);
        }
    }

    private void saveTokenOrThrow(Response<AuthResponse> response, String fallbackMessage) throws IOException {
        if (!response.isSuccessful() || response.body() == null || response.body().accessToken == null) {
            throw new IOException(apiErrorMessage(response, fallbackMessage));
        }
        AuthResponse authResponse = response.body();
        String name = authResponse.user == null ? "" : authResponse.user.name;
        String email = authResponse.user == null ? "" : authResponse.user.email;
        session.saveSession(authResponse.accessToken, name, email);
    }

    private void requireInternetForAuth() throws IOException {
        if (!NetworkUtils.hasInternet(context)) {
            throw new IOException("Necesitas conexion para iniciar sesion o registrarte");
        }
    }

    private String apiErrorMessage(Response<?> response, String fallbackMessage) {
        if (response.code() == 401) {
            return "Email o contrasena incorrectos";
        }

        try {
            if (response.errorBody() == null) {
                return fallbackMessage;
            }

            ApiErrorResponse apiError = gson.fromJson(response.errorBody().string(), ApiErrorResponse.class);
            if (apiError == null) {
                return fallbackMessage;
            }

            if (apiError.message != null && apiError.message.toLowerCase().contains("email ya esta registrado")) {
                return "El perfil ya estaba creado. Puedes iniciar sesion.";
            }

            if (apiError.validationErrors != null && !apiError.validationErrors.isEmpty()) {
                StringBuilder builder = new StringBuilder("Revisa los datos:");
                for (Map.Entry<String, String> entry : apiError.validationErrors.entrySet()) {
                    builder.append("\n").append(entry.getKey()).append(": ").append(entry.getValue());
                }
                return builder.toString();
            }

            if (apiError.message != null && !apiError.message.isBlank()) {
                return apiError.message;
            }
        } catch (IOException ignored) {
            return fallbackMessage;
        }

        return fallbackMessage;
    }
}
