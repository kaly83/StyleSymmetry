package com.example.interim;

import java.util.List;

public class OpenAIRequest {
    private String model;
    private List<Message> messages;
    private double temperature;

    public OpenAIRequest(String prompt) {
        this.model = "gpt-4";
        this.temperature = 0.7;
        this.messages = List.of(new Message("user", prompt));
    }

    static class Message {
        private String role;
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}

