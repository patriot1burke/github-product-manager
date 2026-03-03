package io.quarkiverse.github.index;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService
public interface SummaryPrompt {

    @SystemMessage("""
            You are a helpful assistant that summarizes documents.  Take the user message and summarize the document text contained within it to no more than 5 bullet points.
            """)
    String summarize(@UserMessage String issue);

}
