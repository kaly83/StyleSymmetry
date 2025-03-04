package com.example.interim;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public class OpenAIService {
    private static final String API_KEY = "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"; // 🔥 Replace with your key
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String ENDPOINT = "https://api.openai.com/v1/chat/completions";

    public static List<String[]> getHexColors(String skinTone, String season) {
        List<String[]> colorPairs = new ArrayList<>();
        OkHttpClient client = new OkHttpClient();

        String prompt = "Provide 5 distinct colors suitable for a person with " + skinTone +
                " skin tone in the " + season + " season. " +
                "Return the response in this format: Color Name - #HEXCODE. Example: Deep Blue - #1A237E";

        JSONObject json = new JSONObject();
        try {
            json.put("model", "gpt-4");
            json.put("messages", new JSONArray()
                    .put(new JSONObject().put("role", "system").put("content", "You are a color expert."))
                    .put(new JSONObject().put("role", "user").put("content", prompt))
            );
            json.put("max_tokens", 150);

            RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url(API_URL)
                    .header("Authorization", "Bearer " + API_KEY)
                    .post(body)
                    .build();

            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                JSONObject responseObject = new JSONObject(response.body().string());
                String content = responseObject.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");

                // Extract color names and hex codes
                String[] lines = content.split("\n");
                for (String line : lines) {
                    if (line.contains(" - #")) {
                        String[] parts = line.split(" - #");
                        if (parts.length == 2) {
                            String colorName = parts[0].trim();
                            String hexCode = "#" + parts[1].trim();
                            if (hexCode.matches("^#([A-Fa-f0-9]{6})$")) { // Ensure valid hex
                                colorPairs.add(new String[]{colorName, hexCode});
                            }
                        }
                    }
                }
            } else {
                Log.e("OpenAI", "API request failed: " + response.body().string());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Fallback colors if API fails
        if (colorPairs.isEmpty()) {
            colorPairs.add(new String[]{"Sunset Orange", "#FF5733"});
            colorPairs.add(new String[]{"Ocean Blue", "#3498DB"});
            colorPairs.add(new String[]{"Emerald Green", "#2ECC71"});
            colorPairs.add(new String[]{"Royal Purple", "#9B59B6"});
            colorPairs.add(new String[]{"Golden Yellow", "#F1C40F"});
        }

        return colorPairs;
    }

    public static String getProfileAnalysis(String skinTone, String season) {
        OkHttpClient client = new OkHttpClient();

        String prompt = "Based on the skin tone '" + skinTone + "' and season '" + season + "', " +
                "suggest an appropriate hex color for styling.";


        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", "gpt-4-turbo"); // Use a valid model name
            jsonBody.put("messages", new JSONArray()
                    .put(new JSONObject().put("role", "system").put("content", "You are a color analysis assistant."))
                    .put(new JSONObject().put("role", "user").put("content", prompt))
            );
            jsonBody.put("temperature", 0.7);

            RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url(ENDPOINT)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    JSONObject responseObject = new JSONObject(response.body().string());

                    // OpenAI now returns the response inside "choices" → "message" → "content"
                    JSONArray choices = responseObject.getJSONArray("choices");
                    if (choices.length() > 0) {
                        return choices.getJSONObject(0).getJSONObject("message").getString("content").trim();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Error fetching color suggestion.";
    }



}
