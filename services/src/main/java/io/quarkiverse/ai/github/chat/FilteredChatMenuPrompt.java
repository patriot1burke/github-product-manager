package io.quarkiverse.ai.github.chat;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.ToolBox;
import io.quarkiverse.langchain4j.chatscopes.InvocationScoped;

@RegisterAiService
@InvocationScoped
public interface FilteredChatMenuPrompt {

    @SystemMessage("""
            You are a RAG chat session manager.  Choose an operation/tool to execute based on the user query
            based on this list:
            1. exit: Finish the user chat session
            2. setMinScore: Set the minimum score threshold for filtering results
            3. resetChatMemory: Clear or reset the chat memory/session.
            4. describeFilter: Describe the filter being used for the chat session

            Do not guess the operation to perform.  Say you do not understand the query if you are not sure
            what the operation to invoke is.

            """)
    @ToolBox(FilteredChat.class)
    Result<String> executeOperation(@UserMessage String msg);
}
