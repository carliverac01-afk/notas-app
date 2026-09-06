package com.example.notesandroid.api;

public class AuthResponse {
    public String tokenType;
    public String accessToken;
    public long expiresInSeconds;
    public UserResponse user;

    public static class UserResponse {
        public long id;
        public String email;
        public String name;
    }
}
