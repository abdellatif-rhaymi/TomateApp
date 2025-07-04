package com.example.tomatosapp.network; // Adapte ton package

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface PredictionApiService {
    @Multipart
    @POST("predict") // Le chemin de ton endpoint Flask (commence par / si c'est la racine du service)
    Call<PredictionResponse> uploadImage(@Part MultipartBody.Part imageFile);
}