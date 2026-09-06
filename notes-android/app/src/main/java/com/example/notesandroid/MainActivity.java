package com.example.notesandroid;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.app.Activity;

import com.example.notesandroid.data.NoteEntity;
import com.example.notesandroid.repository.NetworkUtils;
import com.example.notesandroid.repository.NotesRepository;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private NotesRepository repository;
    private ExecutorService executor;
    private Handler mainHandler;

    private LinearLayout authSection;
    private LinearLayout notesSection;
    private LinearLayout notesContainer;
    private EditText nameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private EditText titleInput;
    private EditText contentInput;
    private TextView statusText;
    private Button saveNoteButton;

    private NoteEntity editingNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = new NotesRepository(this);
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        bindViews();
        configureActions();

        if (repository.isLoggedIn()) {
            showNotes();
            loadNotes();
        } else {
            showAuth();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private void bindViews() {
        authSection = findViewById(R.id.authSection);
        notesSection = findViewById(R.id.notesSection);
        notesContainer = findViewById(R.id.notesContainer);
        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        titleInput = findViewById(R.id.titleInput);
        contentInput = findViewById(R.id.contentInput);
        statusText = findViewById(R.id.statusText);
        saveNoteButton = findViewById(R.id.saveNoteButton);
    }

    private void configureActions() {
        findViewById(R.id.loginButton).setOnClickListener(view -> login());
        findViewById(R.id.registerButton).setOnClickListener(view -> register());
        findViewById(R.id.saveNoteButton).setOnClickListener(view -> saveNote());
        findViewById(R.id.syncButton).setOnClickListener(view -> loadNotes());
        findViewById(R.id.logoutButton).setOnClickListener(view -> {
            repository.logout();
            showAuth();
        });
    }

    private void login() {
        String email = valueOf(emailInput);
        String password = valueOf(passwordInput);

        if (email.isBlank() || password.isBlank()) {
            toast("Ingresa email y contrasena");
            return;
        }

        runTask("Iniciando sesion...", () -> {
            repository.login(email, password);
            mainHandler.post(() -> {
                toast("Sesion iniciada");
                showNotes();
                loadNotes();
            });
        });
    }

    private void register() {
        String name = valueOf(nameInput);
        String email = valueOf(emailInput);
        String password = valueOf(passwordInput);

        if (name.isBlank() || email.isBlank() || password.length() < 6) {
            toast("Completa nombre, email y contrasena de minimo 6 caracteres");
            return;
        }

        runTask("Registrando usuario...", () -> {
            repository.register(name, email, password);
            mainHandler.post(() -> {
                toast("Perfil creado correctamente");
                showNotes();
                loadNotes();
            });
        });
    }

    private void saveNote() {
        String title = valueOf(titleInput);
        String content = valueOf(contentInput);

        if (title.isBlank() || content.isBlank()) {
            toast("Completa titulo y contenido");
            return;
        }

        runTask("Guardando nota...", () -> {
            if (editingNote == null) {
                repository.createNote(title, content);
            } else {
                repository.updateNote(editingNote, title, content);
            }
            mainHandler.post(() -> {
                toast(editingNote == null ? "Nota creada" : "Nota actualizada");
                clearForm();
                loadNotes();
            });
        });
    }

    private void loadNotes() {
        setStatus(NetworkUtils.hasInternet(this) ? "Con conexion: sincronizando" : "Sin conexion: mostrando notas locales");
        executor.execute(() -> {
            try {
                List<NoteEntity> notes = repository.getNotes();
                mainHandler.post(() -> renderNotes(notes));
            } catch (IOException exception) {
                mainHandler.post(() -> {
                    toast(exception.getMessage());
                    setStatus("No se pudo sincronizar. Se mantienen las notas locales.");
                });
            }
        });
    }

    private void renderNotes(List<NoteEntity> notes) {
        notesContainer.removeAllViews();
        setStatus(NetworkUtils.hasInternet(this) ? "Notas sincronizadas" : "Modo offline");

        if (notes.isEmpty()) {
            TextView empty = simpleText("No tienes notas guardadas.");
            notesContainer.addView(empty);
            return;
        }

        for (NoteEntity note : notes) {
            notesContainer.addView(noteView(note));
        }
    }

    private View noteView(NoteEntity note) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.note_background);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(cardParams);

        TextView title = simpleText(note.title);
        title.setTextSize(18);
        title.setTextColor(getColor(R.color.text_primary));
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView content = simpleText(note.content);
        content.setTextColor(getColor(R.color.text_secondary));

        TextView state = simpleText(note.pendingSync ? "Pendiente por sincronizar" : "Sincronizada");
        state.setTextSize(12);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        Button editButton = new Button(this);
        editButton.setText("Editar");
        editButton.setOnClickListener(view -> editNote(note));

        Button deleteButton = new Button(this);
        deleteButton.setText("Eliminar");
        deleteButton.setOnClickListener(view -> deleteNote(note));

        actions.addView(editButton);
        actions.addView(deleteButton);

        card.addView(title);
        card.addView(content);
        card.addView(state);
        card.addView(actions);
        return card;
    }

    private void editNote(NoteEntity note) {
        editingNote = note;
        titleInput.setText(note.title);
        contentInput.setText(note.content);
        saveNoteButton.setText("Actualizar nota");
    }

    private void deleteNote(NoteEntity note) {
        runTask("Eliminando nota...", () -> {
            repository.deleteNote(note);
            mainHandler.post(() -> {
                toast("Nota eliminada");
                loadNotes();
            });
        });
    }

    private void clearForm() {
        editingNote = null;
        titleInput.setText("");
        contentInput.setText("");
        saveNoteButton.setText("Guardar nota");
    }

    private void showAuth() {
        authSection.setVisibility(View.VISIBLE);
        notesSection.setVisibility(View.GONE);
        passwordInput.setText("");
    }

    private void showNotes() {
        authSection.setVisibility(View.GONE);
        notesSection.setVisibility(View.VISIBLE);
    }

    private void runTask(String status, BackgroundTask task) {
        setStatus(status);
        executor.execute(() -> {
            try {
                task.run();
            } catch (Exception exception) {
                mainHandler.post(() -> {
                    toast(exception.getMessage());
                    setStatus("Operacion no completada");
                });
            }
        });
    }

    private TextView simpleText(String value) {
        TextView textView = new TextView(this);
        textView.setText(TextUtils.isEmpty(value) ? "" : value);
        textView.setTextSize(15);
        textView.setPadding(0, 4, 0, 4);
        return textView;
    }

    private String valueOf(EditText editText) {
        return editText.getText().toString().trim();
    }

    private void setStatus(String message) {
        statusText.setText(message);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private interface BackgroundTask {
        void run() throws Exception;
    }
}
