package io.quarkiverse.github.pm;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.websockets.next.WebSocketConnection;

@SessionScoped
public class ChatContext {

    @Inject
    WebSocketConnection connection;

    @Inject
    ObjectMapper objectMapper;

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
