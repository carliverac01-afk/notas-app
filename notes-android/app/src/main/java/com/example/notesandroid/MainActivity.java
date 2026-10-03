package com.example.notesandroid;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.notesandroid.data.NoteEntity;
import com.example.notesandroid.repository.UserSession;

import java.util.List;

public class MainActivity extends ComponentActivity {
    private static final int LOCATION_PERMISSION_REQUEST = 50;

    private NotesViewModel viewModel;

    private LinearLayout authSection;
    private LinearLayout notesSection;
    private LinearLayout notesContainer;
    private EditText nameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private EditText titleInput;
    private EditText contentInput;
    private TextView statusText;
    private TextView userNameText;
    private TextView userEmailText;
    private TextView locationText;
    private Button saveNoteButton;

    private NoteEntity editingNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(NotesViewModel.class);

        bindViews();
        configureActions();
        observeState();
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
        userNameText = findViewById(R.id.userNameText);
        userEmailText = findViewById(R.id.userEmailText);
        locationText = findViewById(R.id.locationText);
        saveNoteButton = findViewById(R.id.saveNoteButton);
    }

    private void configureActions() {
        findViewById(R.id.loginButton).setOnClickListener(view -> login());
        findViewById(R.id.registerButton).setOnClickListener(view -> register());
        findViewById(R.id.saveNoteButton).setOnClickListener(view -> saveNote());
        findViewById(R.id.syncButton).setOnClickListener(view -> viewModel.loadNotes());
        findViewById(R.id.locationButton).setOnClickListener(view -> requestLocationWhenNeeded());
        findViewById(R.id.logoutButton).setOnClickListener(view -> {
            clearForm();
            clearLocation();
            viewModel.logout();
        });
    }

    private void observeState() {
        viewModel.authenticated().observe(this, authenticated -> {
            if (Boolean.TRUE.equals(authenticated)) {
                showNotes();
            } else {
                showAuth();
            }
        });

        viewModel.userSession().observe(this, this::renderUser);
        viewModel.notes().observe(this, this::renderNotes);
        viewModel.status().observe(this, status -> statusText.setText(status == null ? "" : status));
        viewModel.message().observe(this, message -> {
            if (message != null && !message.isBlank()) {
                toast(message);
            }
        });
    }

    private void login() {
        String email = valueOf(emailInput);
        String password = valueOf(passwordInput);

        if (email.isBlank() || password.isBlank()) {
            toast("Ingresa email y contrasena");
            return;
        }

        viewModel.login(email, password);
    }

    private void register() {
        String name = valueOf(nameInput);
        String email = valueOf(emailInput);
        String password = valueOf(passwordInput);

        if (name.isBlank() || email.isBlank() || password.length() < 6) {
            toast("Completa nombre, email y contrasena de minimo 6 caracteres");
            return;
        }

        viewModel.register(name, email, password);
    }

    private void saveNote() {
        String title = valueOf(titleInput);
        String content = valueOf(contentInput);

        if (title.isBlank() || content.isBlank()) {
            toast("Completa titulo y contenido");
            return;
        }

        viewModel.saveNote(editingNote, title, content);
        clearForm();
    }

    private void renderUser(UserSession userSession) {
        if (userSession == null) {
            userNameText.setText("");
            userEmailText.setText("");
            return;
        }

        userNameText.setText("Hola, " + userSession.displayName());
        userEmailText.setText(userSession.email == null ? "" : userSession.email);
    }

    private void renderNotes(List<NoteEntity> notes) {
        notesContainer.removeAllViews();

        if (notes == null || notes.isEmpty()) {
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
        deleteButton.setOnClickListener(view -> viewModel.deleteNote(note));

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

    private void requestLocationWhenNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            showSingleLocation();
            return;
        }

        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST
        );
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showSingleLocation();
            } else {
                locationText.setText("Permiso de ubicacion denegado");
            }
        }
    }

    private void showSingleLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            locationText.setText("No se pudo acceder al servicio de ubicacion");
            return;
        }

        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location == null) {
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        if (location == null) {
            locationText.setText("Ubicacion no disponible por ahora");
            return;
        }

        String text = String.format(
                "Ubicacion: %.5f, %.5f",
                location.getLatitude(),
                location.getLongitude()
        );
        locationText.setText(text);
    }

    private void clearForm() {
        editingNote = null;
        titleInput.setText("");
        contentInput.setText("");
        saveNoteButton.setText("Guardar nota");
    }

    private void clearLocation() {
        locationText.setText("");
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

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
