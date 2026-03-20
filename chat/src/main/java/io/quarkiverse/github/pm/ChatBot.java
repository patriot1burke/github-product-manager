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
    CommandPrompt mainPrompt;

    @OnOpen
    public void onOpen() {
        chatContext.console("Connected to chat");
    }

    static AppLogger logger = AppLogger.getLogger(ChatBot.class);

    @OnTextMessage
    @Blocking
    public void onMessage(String message) throws Exception {
        Log.info("User Message: " + message);
        try {

            if (chatContext.currentChat() == null) {
                Result<String> result = mainPrompt.execute(message);
                if (result.content() != null) {
                    chatContext.markdown(result.content());
                }
            } else {
                Result<String> response = chatContext.currentChat().chat(message);
                if (response.content() != null) {
                    chatContext.markdown(response.content());
                }
            }
        } catch (Exception e) {
            Log.error(e);
            chatContext.message("An error occurred, try again.");
        }
    }

    @OnClose
    public void onClose() {
        Log.info("Client disconnected");
    }
}
