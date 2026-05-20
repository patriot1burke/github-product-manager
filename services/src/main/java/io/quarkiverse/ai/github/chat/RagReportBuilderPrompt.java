package io.quarkiverse.ai.github.chat;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.ToolBox;
import io.quarkiverse.langchain4j.chatscopes.ChatScoped;

@RegisterAiService
@ChatScoped
public interface RagReportBuilderPrompt {
    public static final String CHAT_ROUTE = "rag-report-builder";

    @SystemMessage("""
            Guide the user through building a report for analysis of GitHub repositories.

            The following details are required:
            - The name of the report
            - The prompt to use when generating the report
            - The name of the filter to use to generate the report

            The user can also ask to test or execute the report.  Call the 'execute' tool to test the report.
            """)
    @ToolBox(RagReportBuilder.class)
    Result<String> build(@UserMessage String msg);
}
