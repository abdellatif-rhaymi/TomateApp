package com.example.tomatosapp.network; // Adapte ton package

import com.google.gson.annotations.SerializedName;

public class PredictionResponse {
    @SerializedName("predicted_label")
    private String predictedLabel;

    @SerializedName("confidence")
    private float confidence;

    @SerializedName("error") // Pour gérer les erreurs retournées par ton API Flask
    private String error;

    public String getPredictedLabel() {
        return predictedLabel;
    }

    public float getConfidence() {
        return confidence;
    }

    public String getError() {
        return error;
    }
}