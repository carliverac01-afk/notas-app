package com.example.notesandroid.api;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface NotesApi {
    @POST("api/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @POST("api/auth/login")
    Call<AuthResponse> login(@Body AuthRequest request);

    @GET("api/notes")
    Call<List<NoteResponse>> getNotes(@Header("Authorization") String authorization);

    @GET("api/notes/sync")
    Call<List<NoteResponse>> syncNotes(
            @Header("Authorization") String authorization,
            @Query("updatedAfter") String updatedAfter
    );

    @POST("api/notes")
    Call<NoteResponse> createNote(
            @Header("Authorization") String authorization,
            @Body NoteRequest request
    );

    @PUT("api/notes/{id}")
    Call<NoteResponse> updateNote(
            @Header("Authorization") String authorization,
            @Path("id") long id,
            @Body NoteRequest request
    );

    @DELETE("api/notes/{id}")
    Call<Void> deleteNote(
            @Header("Authorization") String authorization,
            @Path("id") long id
    );
}
