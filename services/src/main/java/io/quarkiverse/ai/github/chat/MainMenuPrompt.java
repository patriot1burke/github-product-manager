package io.quarkiverse.ai.github.chat;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.ToolBox;
import io.quarkiverse.langchain4j.chatscopes.ChatRoute;
import io.quarkiverse.langchain4j.chatscopes.ChatScoped;
import io.quarkiverse.langchain4j.chatscopes.DefaultChatRoute;

@RegisterAiService
@ChatScoped
public interface MainMenuPrompt {

    @SystemMessage("""
            Help the user choose between these actions:
            
            * Create a filter.
            * Open a query session with the AI.  The user must provide a filter name to use for the session.
            * List the filters that have been created
            
            # Examples
            User: Create a filter
            Action: Call the 'createFilter" tool
            
            User: Chat with filter hibernate
            Action: Call the 'chatWithFilter' tool
            
            User: List filters
            Action: Call the 'listFilters' tool
            """)
    @ToolBox( MainMenuTools.class)
    @ChatRoute("main-menu")
    @DefaultChatRoute
    String build(@UserMessage String msg);
}
