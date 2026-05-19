package io.quarkiverse.ai.github.chat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.ai.github.api.Github;
import io.quarkiverse.ai.github.db.*;
import io.quarkiverse.ai.github.scanner.model.GitType;
import io.quarkiverse.ai.github.scanner.model.TimePeriod;
import io.quarkiverse.ai.github.util.AppLogger;
import io.quarkiverse.graphql.client.QueryError;
import io.quarkiverse.langchain4j.chatscopes.ChatRoute;
import io.quarkiverse.langchain4j.chatscopes.ChatRouteContext;
import io.quarkiverse.langchain4j.chatscopes.ChatScope;
import io.quarkiverse.langchain4j.chatscopes.ChatScoped;

@ChatScoped
public class FilterBuilder {
    static AppLogger log = AppLogger.getLogger(FilterBuilder.class);

    public record LabelInfo(String name, String description) {
    }

    @Inject
    GithubLabelRepository labelRepository;

    @Inject
    EmbeddingsRepository embeddings;

    @Inject
    ChatRouteContext ctx;

    @Inject
    FilterBuilderPrompt prompt;

    @Inject
    Github github;

    private List<RepositoryFilter> changes = new ArrayList<>();
    private Map<String, LabelInfo> cachedLabels;
    private String repository;
    private boolean finished = false;

    @ChatRoute(FilterBuilderPrompt.CHAT_ROUTE)
    public void build(@UserMessage String msg) {
        int size = changes.size();
        Result<String> result = prompt.build(msg);
        if (finished) {
            ChatScope.pop();
            ctx.response().event(ChatWindow.POP_CHAT_WINDOW, "");
            return;
        }
        if (changes.size() != size) {
            outputFilter(changes.getLast());
        }
        if (result.content() != null) {
            ctx.response().message(result.content());
        }
        ctx.response().thinking("Type 'finished' or 'cancel' to end the building process");

    }

    @Tool("Set the github repository")
    public void setRepository(String repository) {
        try {
            github.repository(repository).labels();
        } catch (QueryError e) {
            if (e.errorNode().toString().contains("NOT_FOUND")) {
                throw new IllegalArgumentException("Unknown repository.");
            } else {
                log.error("Error validating repository", e);
                return;
            }
        }
        ctx.response().thinking("Setting repository to " + repository);
        this.repository = repository;
        cachedLabels = null;
        RepositoryFilter filter = next();
        filter.repository = repository;
    }

    @Tool("List the labels (categories) and their descriptions that are available in the repository for categorization")
    public List<LabelInfo> getLabels() {
        if (cachedLabels != null) {
            ctx.response().thinking("Searching labels");
        }
        pullLabels();
        return new ArrayList<>(cachedLabels.values());
    }

    private void pullLabels() {
        if (cachedLabels == null) {
            if (repository == null) {
                throw new IllegalStateException("No repository set");
            }
            ctx.response().thinking("Pulling labels");
            labelRepository.syncLabels(repository);
            cachedLabels = new HashMap<>();
            labelRepository.findByRepository(repository)
                    .forEach(l -> cachedLabels.put(l.name, new LabelInfo(l.name, l.description)));
        }
    }

    private RepositoryFilter next() {
        RepositoryFilter filter = changes.isEmpty() ? null : changes.getLast();
        if (filter == null) {
            filter = new RepositoryFilter();

        } else {
            filter = filter.copy();
        }
        changes.add(filter);
        return filter;
    }

    public void outputFilter(RepositoryFilter filter) {
        ctx.response().thinking("\nFilter:");
        ctx.response().thinking(filter.output());
        if (filter.repository != null) {
            int count = embeddings.filterCount(filter);
            ctx.response().thinking("Filter touches " + count + " entries");
        }
        ctx.response().thinking("Type 'undo' to undo your last changes.");
    }

    @Tool("set the description of the filter you are creating")
    public void setDescription(String description) {
        ctx.response().thinking("Setting description to " + description);
        RepositoryFilter filter = next();
        filter.description = description;

    }

    @Tool("set the name of the filter you are creating")
    public void setName(String name) {
        try {
            ctx.response().thinking("Setting name to " + name);
            RepositoryFilter filter = next();
            filter.name = name;
            outputFilter(filter);
        } catch (Exception e) {
            log.error("Error setting name", e);
            throw new RuntimeException(e);
        }
    }

    @Tool("add a label to the filter that must be present for a repository to match (and)")
    public void andHasLabel(String label) {
        try {
            ctx.response().thinking("Adding required label " + label);
            checkLabel(label);

            RepositoryFilter filter = next();
            if (filter.filters == null) {
                filter.filters = new Filters();
            }
            filter.filters.andLabels.add(label);
            outputFilter(filter);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error adding label", e);
            throw new RuntimeException(e);
        }
    }

    private void checkLabel(String label) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Label cannot be null or blank");
        }
        pullLabels();
        if (!cachedLabels.containsKey(label)) {
            throw new IllegalArgumentException(
                    "Label not found.  Obtain labels from getLabels() tool call and ask user to pick from that list");
        }
    }

    @Tool("add a label to the filter that does not have to be a match (or)")
    public void orHasLabel(String label) {
        ctx.response().thinking("Adding optional label " + label);
        checkLabel(label);
        RepositoryFilter filter = next();
        if (filter.filters == null) {
            filter.filters = new Filters();
        }
        filter.filters.orLabels.add(label);
    }

    @Tool("Only discussions should be used in the filter")
    public void justDiscussions() {
        ctx.response().thinking("Only discussions should be used in the filter");
        RepositoryFilter filter = next();
        filter.type = GitType.DISCUSSION;

    }

    @Tool("set entry type: discussion, issue, or all")
    public void setEntryType(GitType type) {
        ctx.response().thinking("Setting entry type to " + type);
        RepositoryFilter filter = next();
        filter.type = type;
    }

    @Tool("Only issues should be used in the filter")
    public void justIssues() {
        ctx.response().thinking("Only issues should be used in the filter");
        RepositoryFilter filter = next();
        filter.type = GitType.ISSUE;
    }

    @Tool("The filter should look for entires that have been updated since a certain time period, i.e 1 week ago")
    public void updatedSince(TimePeriod period) {
        ctx.response().thinking("Looking for entries updated since " + period);
        RepositoryFilter filter = next();
        filter.updatedSince = period;
    }

    @Tool("The filter should look for entires that have been created since a certain time period, i.e 1 week ago")
    public void createdSince(TimePeriod period) {
        ctx.response().thinking("Looking for entries created since " + period);
        RepositoryFilter filter = next();
        filter.createdSince = period;
    }

    @Tool("Set the minScore for filter results")
    public void minScore(double score) {
        ctx.response().thinking("Setting minScore to " + score);
        RepositoryFilter filter = next();
        filter.minScore = score;
    }

    @Tool("Undo the last action")
    public void undoLast() {
        if (changes.isEmpty()) {
            return;
        }
        changes.removeLast();
        ctx.response().thinking("Undoing last action");
    }

    @Tool(value = "Finish building the filter and return the final result", returnBehavior = ReturnBehavior.IMMEDIATE)
    @Transactional
    public void finish() {
        ctx.response().thinking("Finishing filter");
        if (changes.isEmpty())
            throw new IllegalStateException("No filter has been created");
        RepositoryFilter filter = changes.getLast();
        if (filter.name == null || filter.name.isBlank())
            throw new IllegalStateException("Filter must have a name");
        if (filter.description == null || filter.description.isBlank())
            throw new IllegalStateException("Filter must have a description");
        RepositoryFilterKey key = new RepositoryFilterKey(filter.repository, filter.name);
        RepositoryFilter.persist(filter);
        ctx.response().message("Created filter: " + filter.name);
        finished = true;

    }

    @Tool(value = "Cancel the filter building process", returnBehavior = ReturnBehavior.IMMEDIATE)
    public void cancel() {
        ctx.response().message("Filter building cancelled");
        finished = true;
    }
}
