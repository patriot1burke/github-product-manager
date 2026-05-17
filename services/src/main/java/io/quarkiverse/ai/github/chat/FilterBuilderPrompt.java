package io.quarkiverse.ai.github.chat;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.ToolBox;
import io.quarkiverse.langchain4j.chatscopes.ChatScoped;

@RegisterAiService
@ChatScoped
public interface FilterBuilderPrompt {
    public static final String CHAT_ROUTE = "filter-builder";

    @SystemMessage("""
            Guide the user through building a filter through natural language.
            The filter is used to define the criteria for a GitHub repository search.

            # Rules
            * The user must specify a repository, name, and description for the filter
            * The user can also specify catoregories (labels) that are applied as criteria.
            * These label names and descriptions are provided by tooling.
            * Labels/category criteria can be specified by name or description.
            * The labels/categories can be specified as required or optional(and or).
            * When the user says they are finished call the 'finished' tool.
            * If the user decides to cancel or abort the creation of this filter, call the 'cancel' tool.
            """)
    @ToolBox(FilterBuilder.class)
    Result<String> build(@UserMessage String msg);
}
