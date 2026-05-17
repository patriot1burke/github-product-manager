package io.quarkiverse.ai.github.chat;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.ToolBox;
import io.quarkiverse.langchain4j.chatscopes.ChatScoped;

@RegisterAiService(retrievalAugmentor = FilteredChat.class)
@ChatScoped
public interface FilteredChatPrompt {
    public static final String CHAT_ROUTE = "filtered-chat";

    @SystemMessage("""
            Answer questions and create reports from the list of Github issues and discussions provided.
            """)
    @ToolBox(FilteredChat.class)
    String chatWithFilter(@UserMessage String msg);
}
