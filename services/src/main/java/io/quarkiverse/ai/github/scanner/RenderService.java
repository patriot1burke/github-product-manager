package io.quarkiverse.ai.github.scanner;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.jspecify.annotations.NonNull;

import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import io.quarkiverse.ai.github.scanner.model.DiscussionModel;
import io.quarkiverse.ai.github.scanner.model.IssueModel;
import io.quarkiverse.ai.github.scanner.model.RenderResult;
import io.quarkiverse.ai.github.util.AppLogger;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateContents;
import io.quarkus.qute.TemplateInstance;

@ApplicationScoped
public class RenderService {
    static AppLogger log = AppLogger.getLogger(RenderService.class);

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
                    """)
        public static native TemplateInstance discussionBody(DiscussionModel discussion);

        @TemplateContents("""
                # Title: {discussion.title}
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
        public static native TemplateInstance discussionComments(DiscussionModel discussion);

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
                    """)
        public static native TemplateInstance issueBody(IssueModel issue);

        @TemplateContents("""
                # Title: {issue.title}

                # Comments

                {#for comment in issue.comments}
                ## Author: {comment.author}
                {comment.body}
                {/for}
                    """)
        public static native TemplateInstance issueComments(IssueModel issue);
    }

    @Inject
    Summarization summarization;
    private static final int MAX_EMBEDDING_TOKENS = 8191;
    public static final int MAX_WORDS = (int) (MAX_EMBEDDING_TOKENS * 0.90 * 0.75);

    public RenderResult discussion(DiscussionModel discussion) {
        String text = Templates.discussion(discussion).render();
        int tokenCount = estimator.estimateTokenCountInText(text);
        return new RenderResult(text, tokenCount);
    }

    public RenderResult issue(IssueModel issue) {
        String text = Templates.issue(issue).render();
        int tokenCount = estimator.estimateTokenCountInText(text);
        return new RenderResult(text, tokenCount);
    }

    public List<RenderResult> splitDocument(DiscussionModel discussion) {
        String text = Templates.discussion(discussion).render();
        String body = Templates.discussionBody(discussion).render();
        String comments = Templates.discussionComments(discussion).render();
        String type = "discussion";
        return splitDocument(text, body, comments, type);
    }

    public List<RenderResult> splitDocument(IssueModel issue) {
        String text = Templates.issue(issue).render();
        String body = Templates.issueBody(issue).render();
        String comments = Templates.issueComments(issue).render();
        String type = "issue";
        return splitDocument(text, body, comments, type);
    }

    private @NonNull List<RenderResult> splitDocument(String text, String body, String comments, String type) {
        int tokenCount = estimator.estimateTokenCountInText(text);
        if (tokenCount < MAX_EMBEDDING_TOKENS) {
            return List.of(new RenderResult(text, tokenCount));
        }
        int bodyCount = estimator.estimateTokenCountInText(body);
        int commentsCount = estimator.estimateTokenCountInText(comments);
        if (bodyCount > MAX_EMBEDDING_TOKENS && commentsCount > MAX_EMBEDDING_TOKENS) {
            log.thinking("Summarizing " + type + " of " + tokenCount + " tokens");
            String summary = summarization.summarize(text, MAX_WORDS);
            tokenCount = estimator.estimateTokenCountInText(summary);
            log.thinking("Summarized " + type + ": " + tokenCount);
            return List.of(new RenderResult(summary, tokenCount));
        }
        log.thinking("Splitting " + type + " into body and comments chunks");
        List<RenderResult> results = new ArrayList<>();
        if (bodyCount > MAX_EMBEDDING_TOKENS) {
            log.thinking("Summarizing body of " + bodyCount + " tokens");
            body = summarization.summarize(body, MAX_WORDS);
            bodyCount = estimator.estimateTokenCountInText(body);
            log.thinking("Summarized body: " + bodyCount);
            results.add(new RenderResult(body, bodyCount));
        } else {
            results.add(new RenderResult(body, bodyCount));
        }
        if (commentsCount > MAX_EMBEDDING_TOKENS) {
            log.thinking("Summarizing comments of " + commentsCount + " tokens");
            comments = summarization.summarize(comments, MAX_WORDS);
            commentsCount = estimator.estimateTokenCountInText(comments);
            log.thinking("Summarized comments: " + commentsCount);
            results.add(new RenderResult(comments, commentsCount));
        } else {
            results.add(new RenderResult(comments, commentsCount));
        }
        return results;
    }

    public RenderResult summarizeEmbedding(String text) {
        int tokenCount = estimator.estimateTokenCountInText(text);
        return summarizeEmbedding(new RenderResult(text, tokenCount));
    }

    /**
     * If the embedding is too large, summarize it.
     *
     * @param renderResult
     * @return
     */
    public RenderResult summarizeEmbedding(RenderResult renderResult) {
        if (renderResult.tokenCount() > MAX_EMBEDDING_TOKENS) {
            log.thinking("Summarizing text: " + renderResult.tokenCount());
            // max words
            String summary = summarization.summarize(renderResult.text(), MAX_WORDS);
            int tokenCount = estimator.estimateTokenCountInText(summary);
            log.thinking("Summarized text: " + tokenCount);
            return new RenderResult(summary, tokenCount);
        }
        return renderResult;
    }

    OpenAiTokenCountEstimator estimator = new OpenAiTokenCountEstimator(OpenAiChatModelName.GPT_5_1);

    @Produces
    public TokenCountEstimator tokenCountEstimator() {

        return estimator;
    }
}
