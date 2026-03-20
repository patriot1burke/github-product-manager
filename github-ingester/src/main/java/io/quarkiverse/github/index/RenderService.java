package io.quarkiverse.github.index;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import io.quarkiverse.github.index.model.ChangeSet;
import io.quarkiverse.github.index.model.DiscussionModel;
import io.quarkiverse.github.index.model.IssueModel;
import io.quarkiverse.github.index.model.RenderResult;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateContents;
import io.quarkus.qute.TemplateInstance;

@ApplicationScoped
public class RenderService {

    @CheckedTemplate
    public static class Templates {
        @TemplateContents("""
                # Title: {discussion.title}
                ## Author: {discussion.author}
                {#if discussion.category != null}
                ## Category
                {discussion.category}
                {/if}
                {#if discussion.labels.size() > 0}
                ## Labels
                {#each discussion.labels}
                {it}
                {/each}
                {/if}

                # Discussion Body

                {discussion.body}

                # Comments

                {#for comment in discussion.comments}
                ## Author: {comment.author}
                {comment.body}
                {#if comment.replies.size() > 0}
                ## Replies:
                {#for reply in comment.replies}
                ### Author: {reply.author}
                {reply.body}
                {/for}
                {/if}
                {/for}
                    """)
        public static native TemplateInstance discussion(DiscussionModel discussion);

        @TemplateContents("""
                # Title: {issue.title}
                ## Author: {issue.author}
                {#if issue.labels.size() > 0}
                ## Labels:
                {#each issue.labels}
                {it}
                {/each}
                {/if}

                # Issue Body

                {issue.body}

                # Comments

                {#for comment in issue.comments}
                ## Author: {comment.author}
                {comment.body}
                {/for}
                    """)
        public static native TemplateInstance issue(IssueModel issue);
    }

    record RenderEntry(String text, int tokenCount, long updatedAt) {

    }

    static class RenderCache {
        public Map<Integer, RenderEntry> discussions = new ConcurrentHashMap<>();
        public Map<Integer, RenderEntry> issues = new ConcurrentHashMap<>();
    }

    Map<String, RenderCache> renderCacheMap = new ConcurrentHashMap<>();

    public RenderResult discussion(DiscussionModel discussion) {
        RenderCache renderCache = renderCacheMap.computeIfAbsent(discussion.repo(), k -> new RenderCache());
        RenderEntry renderResult = renderCache.discussions.computeIfAbsent(discussion.number(), k -> {
            return buildEntry(discussion);
        });
        if (renderResult.updatedAt() != discussion.updatedAt()) {
            renderResult = buildEntry(discussion);
            renderCache.discussions.put(discussion.number(), renderResult);
        }
        return new RenderResult(renderResult.text(), renderResult.tokenCount());
    }

    private RenderEntry buildEntry(DiscussionModel discussion) {
        String text = Templates.discussion(discussion).render();
        int tokenCount = estimator.estimateTokenCountInText(text);
        return new RenderEntry(text, tokenCount, discussion.updatedAt());
    }

    private RenderEntry buildEntry(IssueModel issue) {
        String text = Templates.issue(issue).render();
        int tokenCount = estimator.estimateTokenCountInText(text);
        return new RenderEntry(text, tokenCount, issue.updatedAt());
    }

    public RenderResult issue(IssueModel issue) {
        RenderCache renderCache = renderCacheMap.computeIfAbsent(issue.repo(), k -> new RenderCache());
        RenderEntry renderResult = renderCache.issues.computeIfAbsent(issue.number(), k -> {
            return buildEntry(issue);
        });
        if (renderResult.updatedAt() != issue.updatedAt()) {
            renderResult = buildEntry(issue);
            renderCache.issues.put(issue.number(), renderResult);
        }
        return new RenderResult(renderResult.text(), renderResult.tokenCount());
    }

    public void prune(String repoName, ChangeSet changeSet) {
        RenderCache renderCache = renderCacheMap.get(repoName);
        if (renderCache != null) {
            renderCache.discussions.keySet().removeAll(changeSet.discussions());
            renderCache.issues.keySet().removeAll(changeSet.issues());
        }
    }

    public void clear(String repoName) {
        renderCacheMap.remove(repoName);
    }

    public void clearAll() {
        renderCacheMap.clear();
    }

    OpenAiTokenCountEstimator estimator = new OpenAiTokenCountEstimator(OpenAiChatModelName.GPT_5_1);

    @Produces
    public TokenCountEstimator tokenCountEstimator() {

        return estimator;
    }
}
