package io.quarkiverse.ai.github.chat;

import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import io.quarkiverse.ai.github.db.GithubLabelRepository;
import io.quarkiverse.ai.github.db.RepositoryFilter;
import io.quarkiverse.ai.github.db.RepositoryFilterKey;
import io.quarkiverse.ai.github.scanner.model.GitType;
import io.quarkiverse.ai.github.scanner.model.TimePeriod;
import io.quarkiverse.langchain4j.chatscopes.ChatRouteContext;
import io.quarkiverse.langchain4j.chatscopes.ChatScope;
import io.quarkiverse.langchain4j.chatscopes.ChatScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ChatScoped
public class RepositoryFilterBuilder {

    public record LabelInfo(String name, String description) {}

    @Inject
    GithubLabelRepository labelRepository;

    @Inject
    ChatRouteContext context;

    private List<RepositoryFilter> changes = new ArrayList<>();
    private Map<String, LabelInfo> cachedLabels;
    private String repository;

    public void setRepository(String repository) {
        this.repository = repository;
        cachedLabels = null;
        RepositoryFilter filter = next();
        filter.repository = repository;
        outputFilter(filter);
    }

    @Tool("List the labels (categories) and their descriptions that are available in the repository for categorization")
    public List<LabelInfo> getLabels() {
        pullLabels();
        return new ArrayList<>(cachedLabels.values());
    }

    private void pullLabels() {
        if (cachedLabels == null) {
            if (repository == null) {
                throw new IllegalStateException("No repository set");
            }
            labelRepository.syncLabels(repository);
            cachedLabels = new HashMap<>();
            labelRepository.findByRepository(repository)
                    .forEach(l -> cachedLabels.put(l.name, new LabelInfo(l.name, l.description)));
        }
    }

    private RepositoryFilter next() {
        RepositoryFilter filter = changes.getLast();
        if (filter == null) {
            filter = new RepositoryFilter();

        } else {
            filter = filter.copy();
        }
        changes.add(filter);
        return filter;
    }

    public void outputFilter(RepositoryFilter filter) {
        List<String> parts = new ArrayList<>();
        if (filter.repository != null) parts.add("repo = '" + filter.repository + "'");
        if (filter.filters != null) {
            if (filter.filters.type != null) parts.add("type = '" + filter.filters.type.toLowerCase() + "'");
            if (!filter.filters.andLabels.isEmpty())
                parts.add(filter.filters.andLabels.stream()
                        .map(l -> "label = '" + l + "'")
                        .collect(Collectors.joining(" and ")));
            if (!filter.filters.orLabels.isEmpty())
                parts.add(filter.filters.orLabels.stream()
                        .map(l -> "label = '" + l + "'")
                        .collect(Collectors.joining(" or ")));
            if (!filter.filters.andFilters.isEmpty())
                parts.add(filter.filters.andFilters.stream()
                        .map(f -> "filter = '" + f + "'")
                        .collect(Collectors.joining(" and ")));
            if (!filter.filters.orFilters.isEmpty())
                parts.add(filter.filters.orFilters.stream()
                        .map(f -> "filter = '" + f + "'")
                        .collect(Collectors.joining(" or ")));
            if (filter.filters.updatedSince != null) parts.add("updatedSince = '" + filter.filters.updatedSince + "'");
            if (filter.filters.createdSince != null) parts.add("createdSince = '" + filter.filters.createdSince + "'");
        }
        context.response().thinking(String.join("\n", parts));
    }

    @Tool("set the description of the filter you are creating")
    public void setDescription(String description) {
        RepositoryFilter filter = next();
        filter.description = description;
        outputFilter(filter);

    }

    @Tool("set the name of the filter you are creating")
    public void setName(String name) {
        RepositoryFilter filter = next();
        filter.name = name;
        outputFilter(filter);
    }

    @Tool("add a label to the filter that must be present for a repository to match (and)")
    public void andHasLabel(String label) {
        checkLabel(label);

        RepositoryFilter filter = next();
        filter.filters.andLabels.add(label);
        outputFilter(filter);
    }

    private void checkLabel(String label) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Label cannot be null or blank");
        }
        pullLabels();
        if (!cachedLabels.containsKey(label)) {
            throw new IllegalArgumentException("Label not found.  Obtain labels from getLabels() tool call and ask user to pick from that list");
        }
    }

    @Tool("add a label to the filter that does not have to be a match (or)")
    public void orHasLabel(String label) {
        checkLabel(label);
        RepositoryFilter filter = next();
        filter.filters.orLabels.add(label);
        outputFilter(filter);
    }

    @Tool("Only discussions should be used in the filter")
    public void justDiscussions() {
        RepositoryFilter filter = next();
        filter.filters.type = GitType.DISCUSSION.name();
        outputFilter(filter);

    }

    @Tool("Only issues should be used in the filter")
    public void justIssues() {
        RepositoryFilter filter = next();
        filter.filters.type = GitType.ISSUE.name();
        outputFilter(filter);
    }

    @Tool("The filter should look for entires that have been updated since a certain time period, i.e 1 week ago")
    public void updatedSince(TimePeriod period) {
        RepositoryFilter filter = next();
        filter.filters.updatedSince = period;
        outputFilter(filter);
    }

    @Tool("The filter should look for entires that have been created since a certain time period, i.e 1 week ago")
    public void createdSince(TimePeriod period) {
        RepositoryFilter filter = next();
        filter.filters.createdSince = period;
        outputFilter(filter);
    }

    @Tool("Undo the last action")
    public void undoLast() {
        if (changes.isEmpty()) {
            return;
        }
        changes.removeLast();
        context.response().thinking("Undoing last action");
        outputFilter(changes.getLast());
    }

    @Tool(value = "Finish building the filter and return the final result", returnBehavior = ReturnBehavior.IMMEDIATE)
    @Transactional
    public void finishBuild() {
        if (changes.isEmpty()) throw new IllegalStateException("No filter has been created");
        RepositoryFilter filter = changes.getLast();
        if (filter.name == null || filter.name.isBlank()) throw new IllegalStateException("Filter must have a name");
        if (filter.description == null || filter.description.isBlank()) throw new IllegalStateException("Filter must have a description");
        RepositoryFilterKey key = new RepositoryFilterKey(filter.repository, filter.name);
        RepositoryFilter.persist(filter);
        ChatScope.pop();
        context.response().message("Created filter: " + key);

    }

    @Tool(value = "Cancel the filter building process", returnBehavior = ReturnBehavior.IMMEDIATE)
    public void cancel() {
        ChatScope.pop();
        context.response().message("Filter building cancelled");
    }
}
