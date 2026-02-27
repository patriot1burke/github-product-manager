package io.quarkiverse.github.index;

import java.util.Collection;

import jakarta.enterprise.context.ApplicationScoped;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.github.api.Discussions.Discussion;
import io.quarkiverse.github.api.Issues.Issue;
import io.quarkiverse.github.api.Labels.Label;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService
@ApplicationScoped
public interface CalculateLabelsPrompt {

        // NOTE: I could not use Set<String> even though OpenAI 5.2 returned a valid json array.  So instead I parse the String returned using Jackson directly.

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
            The output should be a json array of category names.  For example:
            [ "category1", "category2", "category3" ]
                        """)
    @UserMessage("""
            # Title: {discussion.title}
            ## Author: {discussion.author.login}
            ## Created At: {discussion.createdAt}
            ## Updated At: {discussion.updatedAt}

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
    String labelDiscussion(Collection<Label> labels, Discussion discussion);

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
            The output should be a json array of category names.  For example:
            [ "category1", "category2", "category3" ]
                        """)
    @UserMessage("""
            # Title: {issue.title}
            ## Author: {issue.author.login}
            ## Created At: {issue.createdAt}
            ## Updated At: {issue.updatedAt}

            {issue.body}

            # Comments:

            {#for comment in issue.comments.nodes}
            ## Author: {comment.author.login}

            {comment.body}
            {/for}
            """)
    String labelIssue(Collection<Label> labels, Issue issue);
}
