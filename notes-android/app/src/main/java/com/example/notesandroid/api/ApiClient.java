package com.example.notesandroid.api;

import com.example.notesandroid.BuildConfig;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static NotesApi notesApi;

    private ApiClient() {
    }

    public static NotesApi getNotesApi() {
        if (notesApi == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.API_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            notesApi = retrofit.create(NotesApi.class);
        }
        return notesApi;
    }
}
