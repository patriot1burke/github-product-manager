package io.quarkiverse.ai.github.chat;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.ToolBox;
import io.quarkiverse.langchain4j.chatscopes.ChatScoped;

@RegisterAiService
@ChatScoped
public interface MainMenuPrompt {

    @SystemMessage("""
            Help the user choose between these actions:

            * Create a filter.
            * Open a query session with the AI.  The user must provide a filter name to use for the session.
            * List the filters that have been created
            * create a report
            * list the reports that have been created

            # Examples
            User: Create a filter
            Action: Call the 'createFilter" tool

            User: Chat with filter hibernate
            Action: Call the 'chatWithFilter' tool

            User: List filters
            Action: Call the 'listFilters' tool

            User: Create a report
            Action: Call the 'createReport' tool

            User: List reports
            Action: Call the 'listReports' tool

            User: Run report hibernate
            Action: Call the 'runReport' tool
            """)
    @ToolBox(MainMenuCommands.class)
    Result<String> mainMenu(@UserMessage String msg);
}
