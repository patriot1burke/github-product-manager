package io.quarkiverse.ai.github.chat;

import java.util.List;

import jakarta.inject.Inject;

import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import io.quarkiverse.ai.github.db.EmbeddingsRepository;
import io.quarkiverse.ai.github.db.RepositoryFilter;
import io.quarkiverse.ai.github.scanner.PullCacheService;
import io.quarkiverse.langchain4j.chatscopes.*;

@ChatScoped
public class MainMenuTools {
    @Inject
    PullCacheService pullCacheService;

    @Inject
    FilteredChat filteredChat;

    @Inject
    ChatRouteContext ctx;

    @Inject
    EmbeddingsRepository embeddings;

    @Tool(value = "Create a filter that helps with filtering issues and discussions in a GitHub repository query", returnBehavior = ReturnBehavior.IMMEDIATE)
    public void createFilter() {
        ctx.response().event(ChatWindow.PUSH_CHAT_WINDOW, "Create filter");
        ChatScope.push(FilterBuilderPrompt.CHAT_ROUTE);
        ChatScopeMemory.clearMemory();
        ChatRoutes.execute();
    }

    @Tool("Pull the latest data from GitHub for the given repository")
    public void pullLatest(String repo) {
        pullCacheService.pull(repo, null);
    }

    @Tool(value = "Open a chat session with the named filter", returnBehavior = ReturnBehavior.IMMEDIATE)
    public void chatWithFilter(String filterName) {
        RepositoryFilter filter = RepositoryFilter.findById(filterName);
        if (filter == null) {
            throw new RuntimeException("Filter not found: " + filterName);
        }
        ctx.response().event(ChatWindow.PUSH_CHAT_WINDOW, filterName);
        ChatScopeMemory.clearMemory();
        ChatScope.push(FilteredChatPrompt.CHAT_ROUTE);
        filteredChat.setFilter(filter);
        int count = embeddings.filterCount(filter);
        ctx.response().message("Ask me Github repository questions with the filter " + filterName);
        ctx.response().message(filter.output());
        ctx.response().message("Filter touches " + count + " entries");
    }

    @Tool("List all filters by name")
    public List<String> listFilters() {
        List<RepositoryFilter> filters = RepositoryFilter.findAll().list();
        return filters.stream().map(f -> f.name).toList();
    }
}
