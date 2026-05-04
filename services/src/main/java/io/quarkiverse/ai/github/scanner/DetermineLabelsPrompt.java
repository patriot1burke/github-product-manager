package io.quarkiverse.ai.github.scanner;

import java.util.Collection;
import java.util.Set;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.ai.github.api.Discussions.Discussion;
import io.quarkiverse.ai.github.api.Issues.Issue;
import io.quarkiverse.ai.github.api.Labels.Label;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.chatscopes.InvocationScoped;

@RegisterAiService
@InvocationScoped
public interface DetermineLabelsPrompt {
    // NOTE: To get this to work with Set<String> and OpenAI 5.2, I had to tell the LLM to output categories delimited by a new line.

    @SystemMessage("""
            Act as an expert discussion classifier. Your task is to analyze the provided discussion and assign it to the most relevant categories from the list below based on the provided descriptions.

            ### Categories
            {#each labels}
            {it_index + 1}. **{it.name}**: {it.description}
            {/each}

            ### Instructions
            - Read the discussion carefully.
            - Compare the content against the category name and descriptions.
            - Choose as many categories as are relevant.
            - Output ONLY the category names.
            - If no category fits, output an empty list.

            ### Output Format
            The output should be just the category names delimited by a new line For example:
            category1
            category2
            category3
                        """)
    @UserMessage("""
            # Title: {discussion.title}
            ## Author: {discussion.author.login}

            # Topic:

            {discussion.body}

            # Comments:

            {#for comment in discussion.comments.nodes}
            ## Author: {comment.author.login}

            {comment.body}
            {#if comment.replies.nodes.size() > 0}
            ## Replies:
            {#for reply in comment.replies.nodes}
            ### Author: {reply.author.login}
            {reply.body}
            {/for}
            {/if}
            {/for}
            """)
    Set<String> labelDiscussion(Collection<Label> labels, Discussion discussion);

    @SystemMessage("""
            Act as an expert issue classifier. Your task is to analyze the provided issue and assign it to the most relevant categories from the list below based on the provided descriptions.

            ### Categories
            {#each labels}
            {it_index + 1}. **{it.name}**: {it.description}
            {/each}

            ### Instructions
            - Read the issue carefully.
            - Compare the content against the category name and descriptions.
            - Choose as many categories as are relevant.
            - Output ONLY the category names.
            - If no category fits, output an empty list.\

            ### Output Format
            The output should be just the category names delimited by a new line For example:
            category1
            category2
            category3
                        """)
    @UserMessage("""
            # Title: {issue.title}
            ## Author: {issue.author.login}

            # Issue:

            {issue.body}

            # Comments:

            {#for comment in issue.comments.nodes}
            ## Author: {comment.author.login}

            {comment.body}
            {/for}
            """)
    Set<String> labelIssue(Collection<Label> labels, Issue issue);
}
