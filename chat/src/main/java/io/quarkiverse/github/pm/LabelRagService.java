package io.quarkiverse.github.pm;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.github.index.PullCacheService;
import io.quarkiverse.github.index.RenderService;
import io.quarkiverse.github.index.model.RecordSet;
import io.quarkiverse.github.index.model.RenderResult;
import io.quarkus.logging.Log;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateContents;
import io.quarkus.qute.TemplateInstance;

@ApplicationScoped
public class LabelRagService {
    @Inject
    RenderService renderService;

    @Inject
    ManagedChatService managed;

    @Inject
    PullCacheService pull;

    @CheckedTemplate
    public static class Templates {
        @TemplateContents("""
                    You are a helpful assistant that analyzes a set of GitHub issues and discussions.  Answer questions
                    about the following issues and discussions.  Each issue and discussion is delimited by "--END--" and a newline.

                    # Entries

                    {#each entries}
                    {it}

                    --END--

                    {/each}
                """)
        public static native TemplateInstance analyze(List<String> entries);
    }

    int maxTokens = 270000;

    public ManagedChatService.ManagedChat ragWithLabels(String repoName, Set<String> labels) {
        RecordSet recordSet = pull.findByLabel(repoName, labels);
        List<RenderResult> entries = recordSet.discussions().stream().map(renderService::discussion)
                .collect(Collectors.toList());
        entries.addAll(recordSet.issues().stream().map(renderService::issue).collect(Collectors.toList()));
        int sum = 0;
        for (RenderResult entry : entries) {
            sum += entry.tokenCount();
        }
        Log.infov("Sum of tokens: {0}", sum);
        List<String> entryStrings = entries.stream().map(RenderResult::text).collect(Collectors.toList());
        TemplateInstance template = Templates.analyze(entryStrings);
        String prompt = template.render();
        ManagedChatService.ManagedChat chat = managed.token(prompt, maxTokens);
        return chat;
    }

}
