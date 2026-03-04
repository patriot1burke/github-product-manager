package io.quarkiverse.github.pm;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.websockets.next.WebSocketConnection;

@RequestScoped
public class ChatContext {

    @Inject
    WebSocketConnection connection;

    @Inject
    ObjectMapper objectMapper;

    String userMessage;

    public String userMessage() {
        return userMessage;
    }

    public void userMessage(String message) {
        this.userMessage = message;
    }

    public void thinking(String message) {
        message(new ChatEvent("thinking", message));
    }

    public void message(String message) {
        message(new ChatEvent("message", message));
    }

    public void console(String message) {
        message(new ChatEvent("console", message));
    }

    public void message(ChatEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            connection.sendTextAndAwait(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
