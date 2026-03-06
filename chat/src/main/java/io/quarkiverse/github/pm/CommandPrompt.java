package io.quarkiverse.github.pm;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.ToolBox;

@RegisterAiService
public interface CommandPrompt {

    @SystemMessage("""
            You are a helpful assistant that can help with tasks related to Github.
            Analyze the user request and invoke the appropriate tools that match the user request.
            If the user request is not related to Github, respond with "I'm sorry, I can only help with Github tasks."
            """)
    @ToolBox(CommandToolbox.class)
    Result<String> execute(@UserMessage String msg);

}
