package io.quarkiverse.github.pm;

import java.util.function.Supplier;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class ChatModelSupplier implements Supplier<ChatModel> {
    @Override
    public ChatModel get() {
        return OpenAiChatModel.builder()
                .apiKey(System.getenv("QUARKUS_LANGCHAIN4J_OPENAI_API_KEY"))
                .modelName("gpt-5.2")
                .parallelToolCalls(true)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

}
