package io.quarkiverse.github.pm;

import jakarta.inject.Inject;

import io.quarkus.logging.Log;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;

@WebSocket(path = "/chat")
public class ChatBot {

    @Inject
    ChatContext chatContext;

    @OnOpen
    public void onOpen() {
        chatContext.console("Connected to chat");
    }

    @OnTextMessage
    public void onMessage(String message) throws Exception {
        Log.info("Received message: " + message);
        chatContext.console("Received message: " + message);
        chatContext.thinking("Thinking...");
        Thread.sleep(2000);
        chatContext.thinking("Thinking some more...");
        Thread.sleep(2000);
        chatContext.message("Hello, how are you?");

    }

    @OnClose
    public void onClose() {
        Log.info("Client disconnected");
    }
}
