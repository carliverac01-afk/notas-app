package com.example.notesandroid.api;

public class NoteRequest {
    public String title;
    public String content;

    public NoteRequest(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
