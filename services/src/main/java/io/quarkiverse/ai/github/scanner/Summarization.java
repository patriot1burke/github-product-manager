package io.quarkiverse.ai.github.scanner;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.chatscopes.InvocationScoped;

@RegisterAiService
@InvocationScoped
public interface Summarization {
    @SystemMessage("""
            Create a {maximum} word summary of the provided text.
            """)
    @UserMessage("{text}")
    String summarize(String text, int maximum);
}
