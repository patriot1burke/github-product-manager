package io.quarkiverse.github.index;

import java.util.Set;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.github.api.Discussions.Discussion;
import io.quarkiverse.github.api.Labels.Label;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService
public interface CalculateLabelsPrompt {

    @SystemMessage("""
                You are a helpful assistant that calculates what catagories a discussion should be labeled with.  You will be given a discussion and you will need to return a list of categories that the discussion should be labeled with.
                Chose from this list of categories and their descriptions:
                {#for label in labels}
                - category: {label.name}
                - description: {label.description}
                {/for}
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
    Set<String> prompt(Iterable<Label> labels, Discussion discussion);
}
