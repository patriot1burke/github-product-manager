package io.quarkiverse.ai.github.chat;


import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import io.quarkiverse.ai.github.db.RepositoryFilter;
import io.quarkiverse.ai.github.scanner.PullCacheService;
import io.quarkiverse.langchain4j.chatscopes.ChatRouteContext;
import io.quarkiverse.langchain4j.chatscopes.ChatRoutes;
import io.quarkiverse.langchain4j.chatscopes.ChatScope;
import io.quarkiverse.langchain4j.chatscopes.ChatScoped;
import jakarta.inject.Inject;

import java.util.List;

@ChatScoped
public class MainMenu {
    @Inject
    PullCacheService pullCacheService;

    @Inject
    ChatRouteContext ctx;

    @Tool("Create a filter that helps with filtering issues and discussions in a GitHub repository query")
    public void createFilter() {
        ChatScope.push("filter-builder");
        ChatRoutes.execute();
    }

    @Tool("Pull the latest data from GitHub for the given repository")
    public void pullLatest(String repo) {
        pullCacheService.pull(repo, null);
    }

    @Tool(value = "Open a chat session with the named filter", returnBehavior = ReturnBehavior.IMMEDIATE)
    public void chatWithFilter(String filterName) {
        ctx.response().event(ChatWindow.PUSH_CHAT_WINDOW, "Chat using filter " + filterName);
        ChatScope.push("filtered-chat");


    }

    @Tool("List all filters by name")
    public List<String> listFilters() {
        List<RepositoryFilter> filters = RepositoryFilter.findAll().list();
        return filters.stream().map(f -> f.name).toList();
    }
}
