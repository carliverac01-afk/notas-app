package com.example.notesandroid;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.notesandroid.data.NoteEntity;
import com.example.notesandroid.repository.NetworkUtils;
import com.example.notesandroid.repository.NotesRepository;
import com.example.notesandroid.repository.UserSession;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotesViewModel extends AndroidViewModel {
    private final NotesRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<Boolean> authenticated = new MutableLiveData<>();
    private final MutableLiveData<UserSession> userSession = new MutableLiveData<>();
    private final MutableLiveData<List<NoteEntity>> notes = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> status = new MutableLiveData<>("");
    private final MutableLiveData<String> message = new MutableLiveData<>();

    public NotesViewModel(@NonNull Application application) {
        super(application);
        repository = new NotesRepository(application);
        authenticated.setValue(repository.isLoggedIn());
        if (repository.isLoggedIn()) {
            userSession.setValue(repository.getUserSession());
            loadNotes();
        }
    }

    public LiveData<Boolean> authenticated() {
        return authenticated;
    }

    public LiveData<UserSession> userSession() {
        return userSession;
    }

    public LiveData<List<NoteEntity>> notes() {
        return notes;
    }

    public LiveData<String> status() {
        return status;
    }

    public LiveData<String> message() {
        return message;
    }

    public void login(String email, String password) {
        run("Iniciando sesion...", () -> {
            repository.login(email, password);
            authenticated.postValue(true);
            userSession.postValue(repository.getUserSession());
            message.postValue("Sesion iniciada");
            loadNotes();
        });
    }

    public void register(String name, String email, String password) {
        run("Registrando usuario...", () -> {
            repository.register(name, email, password);
            authenticated.postValue(true);
            userSession.postValue(repository.getUserSession());
            message.postValue("Perfil creado correctamente");
            loadNotes();
        });
    }

    public void loadNotes() {
        String syncStatus = NetworkUtils.hasInternet(getApplication())
                ? "Con conexion: sincronizando"
                : "Sin conexion: mostrando notas locales";
        run(syncStatus, () -> {
            notes.postValue(repository.getNotes());
            status.postValue(NetworkUtils.hasInternet(getApplication()) ? "Notas sincronizadas" : "Modo offline");
        });
    }

    public void saveNote(NoteEntity editingNote, String title, String content) {
        run("Guardando nota...", () -> {
            if (editingNote == null) {
                repository.createNote(title, content);
                message.postValue("Nota creada");
            } else {
                repository.updateNote(editingNote, title, content);
                message.postValue("Nota actualizada");
            }
            loadNotes();
        });
    }

    public void deleteNote(NoteEntity note) {
        run("Eliminando nota...", () -> {
            repository.deleteNote(note);
            message.postValue("Nota eliminada");
            loadNotes();
        });
    }

    public void logout() {
        repository.logout();
        authenticated.setValue(false);
        userSession.setValue(null);
        notes.setValue(new ArrayList<>());
        status.setValue("");
        message.setValue("Sesion cerrada");
    }

    private void run(String currentStatus, BackgroundTask task) {
        status.postValue(currentStatus);
        executor.execute(() -> {
            try {
                task.run();
            } catch (Exception exception) {
                String error = exception.getMessage() == null ? "Operacion no completada" : exception.getMessage();
                message.postValue(error);
                status.postValue("Operacion no completada");
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }

    private interface BackgroundTask {
        void run() throws Exception;
    }
}
