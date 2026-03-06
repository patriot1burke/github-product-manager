package io.quarkiverse.github.pm;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import io.quarkiverse.github.index.PruneService;
import io.quarkiverse.github.index.PullCacheService;
import io.quarkiverse.github.index.ReportService;
import io.quarkiverse.github.index.ReportService.BasicReport;
import io.quarkiverse.github.index.ReportService.LabelReport;
import io.quarkiverse.github.index.model.Earlier;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateContents;
import io.quarkus.qute.TemplateInstance;

@ApplicationScoped
public class CommandToolbox {

    @Inject
    ChatContext chatContext;

    // I would like to use IMMEDIATE, but gpt 5.2 only calls one tool at a time.

    @Tool(value = "set the default Github repository", returnBehavior = ReturnBehavior.IMMEDIATE)
    public void setDefaultRepository(String repository) {
        chatContext.thinking("Setting default Github repository to " + repository);
        chatContext.currentRepository(repository);
    }

    @Inject
    PullCacheService pullService;

    @Inject
    PruneService pruneService;

    @Tool(value = "pull discussions and issues from the current Github repository", returnBehavior = ReturnBehavior.IMMEDIATE)
    public void pull(@P(required = false, value = "The repository to pull from") String repository, boolean prune,
            @P(required = false, value = "How long ago should the pull be?") Earlier earlier) {
        if (repository == null) {
            repository = chatContext.currentRepository();
        }
        if (repository == null) {
            throw new RuntimeException("No repository set");
        }
        String msg = "pull --repo=" + repository + (prune ? " --prune" : "") + " "
                + (earlier != null ? " --" + earlier.toString() : "");
        chatContext.thinking(msg);
        pullService.pull(repository, earlier);
        if (chatContext.currentChat() != null) {
            String chatName = chatContext.currentChatName();
            chatContext.clearCurrentChat();
            chatContext.thinking("Cleared chat window " + chatName + ".");
        }
        if (prune) {
            pruneService.prune(repository, earlier);
        }
        chatContext.message("Pull complete.");
    }

    @Inject
    LabelRagService labelRagService;

    @Tool(value = "create chat window for a set of labels", returnBehavior = ReturnBehavior.IMMEDIATE)
    public void createChatWindow(@P(required = false, value = "The repository to generate a window for") String repository,
            @P(required = true, value = "The labels to generate a window for") Set<String> labels) {
        if (repository == null) {
            repository = chatContext.currentRepository();
        }
        if (repository == null) {
            throw new RuntimeException("No repository set");
        }
        if (labels.isEmpty()) {
            throw new RuntimeException("No labels provided");
        }
        String chatName = repository + ":" + labels.stream().collect(Collectors.joining("-"));
        ManagedChatService.ManagedChat chat = labelRagService.ragWithLabels(repository, labels);
        chatContext.currentChat(chatName, chat);
        chatContext.thinking("Created chat window " + chatName + ".");
    }

    @CheckedTemplate
    public static class Templates {
        @TemplateContents("""
                    Total discussions: {report.discussions().total()}
                    Total issues: {report.issues().total()}
                    Label totals:
                    {#each labelReports}
                        {it.name()}: {it.count()}
                    {/each}
                """)
        public static native TemplateInstance basicReport(BasicReport report, Collection<LabelReport> labelReports);
    }

    @Inject
    ReportService reportService;

    @Tool(value = "Basic report that shows a label count for issues and discussions for a specific repository", returnBehavior = ReturnBehavior.IMMEDIATE)
    public void basicReport(@P(required = false, value = "The repository to generate a report for") String repository) {
        if (repository == null) {
            repository = chatContext.currentRepository();
        }
        if (repository == null) {
            throw new RuntimeException("No repository set");
        }
        BasicReport report = reportService.basicReport(repository);
        TemplateInstance template = Templates.basicReport(report, report.labelCounts());
        String msg = template.render();
        chatContext.markdown(msg);
    }

}
