package com.example.interim;

import java.util.List;

public class OpenAIResponse {
    private List<Choice> choices;

    public String getText() {
        return choices.get(0).message.content;
    }

    static class Choice {
        private Message message;
    }

    static class Message {
        private String role;
        private String content;
    }
}
