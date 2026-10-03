package com.example.notesandroid.repository;

public class UserSession {
    public final String name;
    public final String email;

    public UserSession(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public String displayName() {
        if (name != null && !name.isBlank()) {
            return name;
        }
        return email == null ? "Usuario" : email;
    }
}
