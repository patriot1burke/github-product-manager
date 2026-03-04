package io.quarkiverse.github.pm;

import jakarta.inject.Inject;

import dev.langchain4j.service.Result;
import io.quarkiverse.github.util.AppLogger;
import io.quarkus.logging.Log;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.smallrye.common.annotation.Blocking;

@WebSocket(path = "/chat")
public class ChatBot {

    @Inject
    ChatContext chatContext;

    @Inject
    MainPrompt mainPrompt;

    @OnOpen
    public void onOpen() {
        chatContext.console("Connected to chat");
    }

    static AppLogger logger = AppLogger.getLogger(ChatBot.class);

    @OnTextMessage
    @Blocking
    public void onMessage(String message) throws Exception {
        chatContext.userMessage(message);
        Result<String> result = mainPrompt.execute(message);
        if (result.content() != null) {
            chatContext.message(result.content());
        }
    }

    @OnClose
    public void onClose() {
        Log.info("Client disconnected");
    }
}
