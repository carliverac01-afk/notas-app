package com.example.notesandroid.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface NoteDao {
    @Query("SELECT * FROM notes WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    List<NoteEntity> getActiveNotes();

    @Query("SELECT * FROM notes WHERE pendingSync = 1 ORDER BY localId ASC")
    List<NoteEntity> getPendingNotes();

    @Query("SELECT * FROM notes WHERE remoteId = :remoteId LIMIT 1")
    NoteEntity findByRemoteId(long remoteId);

    @Query("SELECT * FROM notes WHERE localId = :localId LIMIT 1")
    NoteEntity findByLocalId(long localId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(NoteEntity note);

    @Update
    void update(NoteEntity note);

    @Delete
    void delete(NoteEntity note);

    @Query("DELETE FROM notes")
    void deleteAll();
}
