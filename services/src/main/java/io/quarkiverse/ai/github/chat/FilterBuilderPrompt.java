package io.quarkiverse.ai.github.chat;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.ToolBox;
import io.quarkiverse.langchain4j.chatscopes.ChatRoute;
import io.quarkiverse.langchain4j.chatscopes.ChatScoped;

@RegisterAiService
@ChatScoped
public interface FilterBuilderPrompt {

    @SystemMessage("""
            Guide the user through building a filter through natural language.
            The filter is used to define the criteria for a GitHub repository search.
            
            The user must specify a repository, name, and description for the filter
            
            The user can also specify catoregories (labels) that are applied as criteria.
            These label names and descriptions are provided by tooling.
            Labels/category criteria can be specified by name or description.
            The labels/categories can be specified as required or optional.
            """)
    @ToolBox(RepositoryFilterBuilder.class)
    @ChatRoute("filter-builder")
    String build(@UserMessage String msg);
}
