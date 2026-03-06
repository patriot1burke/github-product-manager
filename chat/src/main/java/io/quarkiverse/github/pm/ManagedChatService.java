package io.quarkiverse.github.pm;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;

@ApplicationScoped
public class ManagedChatService {

    @Inject
    ChatModel chatModel;

    interface ChatBot {
        String chat(@UserMessage String userMessage);
    }

    public static class ManagedChat {
        ChatMemory chatMemory;
        ChatBot chat;

        public ManagedChat(String systemMessage, ChatMemory chatMemory) {
            chat = AiServices.builder(ChatBot.class).systemMessage(systemMessage).chatMemoryProvider((ignore) -> chatMemory)
                    .build();
        }

        public String chat(String userMessage) {
            return chat.chat(userMessage);
        }
    }

    public static class TypesafeChat<T> {
        ChatMemory chatMemory;
        T service;

        public TypesafeChat(ChatMemory chatMemory, T service) {
            this.chatMemory = chatMemory;
            this.service = service;
        }

        public T service() {
            return service;
        }
    }

    @Inject
    TokenCountEstimator tokenCountEstimator;

    public ManagedChat windowed(String systemMessage, int windowSize) {
        return new ManagedChat(systemMessage, MessageWindowChatMemory.builder().maxMessages(windowSize).build());
    }

    public ManagedChat token(String systemMessage, int tokenLimit) {
        return new ManagedChat(systemMessage,
                TokenWindowChatMemory.builder().maxTokens(tokenLimit, tokenCountEstimator).build());
    }

    public <T> TypesafeChat<T> windowed(Class<T> intf, int windowSize) {
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder().maxMessages(windowSize).build();
        AiServices<T> builder = AiServices.builder(intf);
        return new TypesafeChat<T>(chatMemory, builder.chatMemoryProvider((ignore) -> chatMemory).build());
    }

    public <T> TypesafeChat<T> token(Class<T> intf, int tokenLimit) {
        TokenWindowChatMemory chatMemory = TokenWindowChatMemory.builder().maxTokens(tokenLimit, tokenCountEstimator).build();
        AiServices<T> builder = AiServices.builder(intf);
        return new TypesafeChat<T>(chatMemory, builder.chatMemoryProvider((ignore) -> chatMemory).build());
    }
}
