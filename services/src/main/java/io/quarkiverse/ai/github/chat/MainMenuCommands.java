package io.quarkiverse.ai.github.chat;

import java.util.List;

import jakarta.inject.Inject;

import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.ai.github.db.EmbeddingsRepository;
import io.quarkiverse.ai.github.db.RagReport;
import io.quarkiverse.ai.github.db.RepositoryFilter;
import io.quarkiverse.ai.github.scanner.PullCacheService;
import io.quarkiverse.langchain4j.chatscopes.*;

@ChatScoped
public class MainMenuCommands {
    @Inject
    PullCacheService pullCacheService;

    @Inject
    FilteredChat filteredChat;

    @Inject
    ChatRouteContext ctx;

    @Inject
    EmbeddingsRepository embeddings;

    @Inject
    MainMenuPrompt menu;

    @ChatRoute("main")
    @DefaultChatRoute
    public Result<String> mainMenu(@UserMessage String msg) {
        if ("create filter".equals(msg)) {
            createFilter();
            return null;
        } else if ("list filters".equals(msg)) {
            List<String> list = listFilters();
            ctx.response().message("**Filters**: " + String.join("\n- ", list));
            return null;
        } else if ("list reports".equals(msg)) {
            List<String> list = listRagReports();
            ctx.response().message("**Reports**: " + String.join("\n- ", list));
            return null;
        }
        return menu.mainMenu(msg);
    }

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

    @Tool("List all reports by name")
    public List<String> listRagReports() {
        List<RagReport> reports = RagReport.listAll();
        return reports.stream().map(f -> f.name).toList();
    }

    @Tool(value = "Create a report", returnBehavior = ReturnBehavior.IMMEDIATE)
    public void createReport() {
        ctx.response().event(ChatWindow.PUSH_CHAT_WINDOW, "Create report");
        ChatScope.push(RagReportBuilderPrompt.CHAT_ROUTE);
        ChatScopeMemory.clearMemory();
        ChatRoutes.execute();
    }

    @Inject
    FilteredChatPrompt filteredChatPrompt;

    @Tool(value = "Run a report", returnBehavior = ReturnBehavior.IMMEDIATE)
    public void runReport(String reportName) {
        RagReport report = RagReport.findById(reportName);
        if (report == null) {
            throw new IllegalStateException("Report not found: " + reportName);
        }

        ChatScopeMemory.clearMemory();

        ChatScope.push();
        try {
            RepositoryFilter filter = RepositoryFilter.findById(report.filter);
            if (filter == null) {
                ctx.response().error(
                        "Filter " + report.filter + " not found for report " + reportName + ".  You need to update the report");
                return;
            }
            ctx.response().message("Running report with prompt:\n\n");
            ctx.response().message(report.prompt);
            ctx.response().message("\n\n");
            filteredChat.setFilter(filter);
            String result = filteredChatPrompt.chatWithFilter(report.prompt);
            ctx.response().message(result);
        } finally {
            ChatScope.pop();
        }
    }

}
