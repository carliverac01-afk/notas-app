package com.example.notesandroid.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class NoteEntity {
    @PrimaryKey(autoGenerate = true)
    public long localId;

    public Long remoteId;
    public String title;
    public String content;
    public String createdAt;
    public String updatedAt;
    public String deletedAt;
    public boolean pendingSync;
    public String pendingAction;

    public boolean isDeleted() {
        return deletedAt != null && !deletedAt.isBlank();
    }
}
