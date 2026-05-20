package io.quarkiverse.ai.github.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.AugmentationRequest;
import dev.langchain4j.rag.AugmentationResult;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import dev.langchain4j.store.embedding.filter.comparison.IsGreaterThan;
import io.quarkiverse.ai.github.db.Filters;
import io.quarkiverse.ai.github.db.RepositoryFilter;
import io.quarkiverse.ai.github.scanner.GithubMetadata;
import io.quarkiverse.ai.github.util.AppLogger;
import io.quarkiverse.langchain4j.chatscopes.*;

@ChatScoped
public class FilteredChat implements Supplier<RetrievalAugmentor> {
    static AppLogger log = AppLogger.getLogger(FilterBuilder.class);

    @Inject
    ChatRouteContext ctx;

    @Inject
    EmbeddingStore<TextSegment> embeddingStore;

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    TokenCountEstimator tokenCountEstimator;

    @Inject
    FilteredChatPrompt prompt;

    @Inject
    FilteredChatMenuPrompt menuPrompt;

    RepositoryFilter filter;
    Filter embeddingFilter;
    double minScore;

    public static final int MAX_INPUT_TOKENS = 120000;
    public static final int MAX_RESULTS = MAX_INPUT_TOKENS / 100;

    @Override
    public RetrievalAugmentor get() {
        return this::augment;
    }

    public void setFilter(RepositoryFilter filter) {
        this.filter = filter;
        this.minScore = filter.minScore;
        embeddingFilter = new IsEqualTo(GithubMetadata.REPOSITORY, filter.repository);
        if (filter.type != null) {
            embeddingFilter = embeddingFilter.and(new IsGreaterThan(GithubMetadata.TYPE, filter.type.name()));
        }
        if (filter.updatedSince != null) {
            embeddingFilter = embeddingFilter
                    .and(new IsGreaterThan(GithubMetadata.UPDATED_AT, filter.updatedSince.fromMillis()));
        }
        if (filter.createdSince != null) {
            embeddingFilter = embeddingFilter
                    .and(new IsGreaterThan(GithubMetadata.CREATED_AT, filter.createdSince.fromMillis()));
        }
        if (filter.filters == null) {
            return;
        }
        Filters filters = filter.filters;
        if (filters.andLabels != null && !filters.andLabels.isEmpty()) {
            for (String label : filters.andLabels) {
                embeddingFilter = embeddingFilter.and(new IsEqualTo(GithubMetadata.label(label), "true"));
            }
        }
        if (filter.filters.orLabels != null && !filter.filters.orLabels.isEmpty()) {
            Filter orLabel = null;
            for (String label : filter.filters.orLabels) {
                if (orLabel == null) {
                    orLabel = new IsEqualTo(GithubMetadata.label(label), "true");
                } else {
                    orLabel = orLabel.or(new IsEqualTo(GithubMetadata.label(label), "true"));
                }
            }
            embeddingFilter = embeddingFilter.and(orLabel);
        }
    }

    @ChatRoute(FilteredChatPrompt.CHAT_ROUTE)
    public void chat(@UserMessage String msg) {
        if (msg.equals("done") || msg.equals("exit")) {
            finishChat();
            return;
        }
        Result<String> toolCall = menuPrompt.executeOperation(msg);
        if (toolCall.content() == null) {
            // a command was executed, so just return
            return;
        }

        String result = prompt.chatWithFilter(msg);
        ctx.response().message(result);
        ctx.response().thinking("Type 'done' to end the chat");

    }

    public AugmentationResult augment(AugmentationRequest augmentationRequest) {
        try {
            ChatMessage chatMessage = augmentationRequest.chatMessage();
            String queryText;
            dev.langchain4j.data.message.UserMessage userMessage;
            if (chatMessage instanceof dev.langchain4j.data.message.UserMessage um) {
                userMessage = um;
            } else {
                throw new IllegalArgumentException("Unsupported message type: " + chatMessage.type());
            }
            queryText = userMessage.singleText();
            Embedding embedding = embeddingModel.embed(queryText).content();
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(embedding)
                    .filter(embeddingFilter)
                    .minScore(minScore)
                    .maxResults(MAX_RESULTS)
                    .build();

            EmbeddingSearchResult<TextSegment> search = embeddingStore.search(request);

            // add token count to embedding metadata and whether full text should be pulled
            // from full entry
            List<Content> contents = new ArrayList<>();
            int totalTokens = tokenCountEstimator.estimateTokenCountInText(queryText);
            int numEntries = 0;
            double lowestScore = -1;
            for (EmbeddingMatch<TextSegment> segment : search.matches()) {
                int tokenCount = tokenCountEstimator.estimateTokenCountInText(segment.embedded().text());
                if (totalTokens + tokenCount > MAX_INPUT_TOKENS) {
                    break;
                }
                totalTokens += tokenCount;
                contents.add(Content.from(segment.embedded().text()));
                numEntries++;
                if (lowestScore == -1 || segment.score() < lowestScore) {
                    lowestScore = segment.score();
                }
            }
            if (search.matches().isEmpty()) {
                ctx.response().thinking("No results found.");
            } else {
                if (contents.size() < search.matches().size()) {
                    ctx.response().thinking(
                            String.format("Input token limit reached.  Using top %d of %d related entries.  max score: %.2f"
                                    + " min score: %.2f", numEntries, search.matches().size(), lowestScore, minScore));
                } else {
                    ctx.response().thinking(String.format("Found %d related entries.  max score: %.2f"
                            + " min score: %.2f", numEntries, lowestScore, minScore));
                }
                ctx.response().thinking("Total input tokens: " + totalTokens);
            }

            StringBuilder text = new StringBuilder(queryText);
            text.append("\n\n");
            text.append("Answer using the following information:\n");
            contents.forEach(c -> text.append(c).append("\n\n"));

            ChatMessage augmentedChatMessage = userMessage.toBuilder()
                    .contents(List.of(TextContent.from(text.toString())))
                    .build();

            return AugmentationResult.builder()
                    .chatMessage(augmentedChatMessage)
                    .contents(contents)
                    .build();
        } catch (Exception e) {
            log.error("Error augmenting query", e);
            throw new RuntimeException(e);
        }
    }

    @Tool(value = "Finish or exit the chat", returnBehavior = ReturnBehavior.IMMEDIATE)
    public void finishChat() {
        ctx.response().thinking("Finishing chat");
        ChatScope.pop();
        ctx.response().event(ChatWindow.POP_CHAT_WINDOW, "");
    }

    @Tool(value = "Reset chat memory", returnBehavior = ReturnBehavior.IMMEDIATE)
    public void resetChatMemory() {
        ctx.response().thinking("Resetting chat memory");
        ChatScopeMemory.clearMemory();
    }

    @Tool(value = "Set the minimum score threshold for filtering results", returnBehavior = ReturnBehavior.IMMEDIATE)
    public void setMinScore(double score) {
        ctx.response().thinking("Minimum score threshold set to " + score);
        minScore = score;
    }

    @Tool(value = "Describe the filter being used for the chat session", returnBehavior = ReturnBehavior.IMMEDIATE)
    public void describeFilter() {
        ctx.response().thinking(filter.output());
        ctx.response().thinking("chat overriding min score: " + minScore);
    }
}
