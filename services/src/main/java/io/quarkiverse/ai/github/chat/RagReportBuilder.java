package io.quarkiverse.ai.github.chat;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.ai.github.db.RagReport;
import io.quarkiverse.ai.github.db.RepositoryFilter;
import io.quarkiverse.langchain4j.chatscopes.ChatRoute;
import io.quarkiverse.langchain4j.chatscopes.ChatRouteContext;
import io.quarkiverse.langchain4j.chatscopes.ChatScope;
import io.quarkiverse.langchain4j.chatscopes.ChatScoped;

@ChatScoped
public class RagReportBuilder {

    @Inject
    ChatRouteContext ctx;

    RagReport report = new RagReport();
    RepositoryFilter filter;

    @Inject
    RagReportBuilderPrompt prompt;

    boolean finished = false;

    @ChatRoute(RagReportBuilderPrompt.CHAT_ROUTE)
    public void build(@UserMessage String msg) {
        Result<String> result = null;
        if ("finished".equals(msg)) {
            finish();
        } else if ("cancel".equals(msg)) {
            cancel();
        } else {
            result = prompt.build(msg);
        }

        if (finished) {
            ChatScope.pop();
            ctx.response().event(ChatWindow.POP_CHAT_WINDOW, "");
            return;
        }
        if (result.content() != null) {
            ctx.response().message(result.content());
        }
        ctx.response().thinking("Type 'finished' or 'cancel' to end the building process");

    }

    @Tool(value = "Finish building the filter and return the final result", returnBehavior = ReturnBehavior.IMMEDIATE)
    @Transactional
    public void finish() {
        ctx.response().thinking("Finishing filter");
        if (report.name == null || report.name.isBlank()) {
            throw new IllegalStateException("Report must have a name");
        }
        if (report.prompt == null || report.prompt.isBlank()) {
            throw new IllegalStateException("Report must have a prompt");
        }
        if (report.filter == null) {
            throw new IllegalStateException("Report must have a filter");
        }
        RagReport.persist(report);
        ctx.response().message("Created report: " + filter.name);
        finished = true;

    }

    @Tool(value = "Cancel the report building process", returnBehavior = ReturnBehavior.IMMEDIATE)
    public void cancel() {
        ctx.response().message("Report building cancelled");
        finished = true;
    }

    @Tool("Set name of report")
    public void setName(String name) {
        ctx.response().thinking("Setting name to " + name);
        report.name = name;
    }

    @Tool("Set prompt for report")
    public void setPrompt(String prompt) {
        ctx.response().thinking("Setting prompt to " + prompt);
        report.prompt = prompt;
    }

    @Tool("Set description for report")
    public void setDescription(String description) {
        ctx.response().thinking("Setting description to " + description);
        report.description = description;
    }

    @Tool("Set filter for report")
    public void setFilter(String filterName) {
        ctx.response().thinking("Setting filter to " + filterName);
        RepositoryFilter filter = RepositoryFilter.findById(filterName);
        if (filter == null) {
            throw new IllegalStateException("Filter not found: " + filterName);
        }
        report.filter = filterName;
        this.filter = filter;
    }

    @Inject
    FilteredChat filteredChat;

    @Inject
    FilteredChatPrompt filteredChatPrompt;

    @Tool("list available filters")
    public List<String> listFilters() {
        List<RepositoryFilter> filters = RepositoryFilter.listAll();
        return filters.stream().map(f -> f.name).collect(Collectors.toList());
    }

    @Tool(value = "Execute and test report", returnBehavior = ReturnBehavior.IMMEDIATE)
    public void execute() {
        if (report.prompt == null) {
            throw new IllegalStateException("Prompt not set");
        }
        if (report.filter == null) {
            throw new IllegalStateException("Filter not set");
        }
        ctx.response().message("Running report with prompt:\n\n");
        ctx.response().message("```\n" + report.prompt + "\n```");
        ctx.response().message("\n\n");
        ChatScope.push();
        try {
            filteredChat.setFilter(filter);
            String result = filteredChatPrompt.chatWithFilter(report.prompt);
            ctx.response().message(result);
        } finally {
            ChatScope.pop();
        }
    }
}
