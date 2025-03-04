package com.example.interim;
import okhttp3.*;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;

public class OpenAIHelper {
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String API_KEY = "your_openai_api_key";  // Replace with your OpenAI API key

    public interface OpenAIResponse {
        void onSuccess(String response);
        void onFailure(String error);
    }

    public static void generateProfileText(String skinTone, String season, OpenAIResponse callback) {
        OkHttpClient client = new OkHttpClient();

        String prompt = "Generate a profile analysis for a person with a " + skinTone + " skin tone and " + season +
                " season. Include: \n" +
                "- Concise description of the skin tone\n" +
                "- Brief profile undertone\n" +
                "- Brief profile depth and contrast\n" +
                "- Short and concise profile tips\n" +
                "- Best and worst color palette recommendations.";

        JSONObject json = new JSONObject();
        try {
            json.put("model", "gpt-3.5-turbo");
            json.put("messages", new JSONArray()
                    .put(new JSONObject().put("role", "system").put("content", "You are an expert in color analysis."))
                    .put(new JSONObject().put("role", "user").put("content", prompt))
            );
            json.put("temperature", 0.7);
        } catch (Exception e) {
            callback.onFailure("Error creating request JSON");
            return;
        }

        RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Request failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onFailure("Response error: " + response.message());
                    return;
                }

                try {
                    JSONObject jsonResponse = new JSONObject(response.body().string());
                    String generatedText = jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
                    callback.onSuccess(generatedText);
                } catch (Exception e) {
                    callback.onFailure("Parsing response failed");
                }
            }
        });
    }
}
