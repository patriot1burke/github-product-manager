package io.quarkiverse.github.index;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkiverse.github.index.model.DiscussionModel;
import io.quarkiverse.github.index.model.IssueModel;
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

                # Topic:

                {discussion.body}

                # Comments:

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

                # Topic:

                {issue.body}

                # Comments:

                {#for comment in issue.comments}
                ## Author: {comment.author}
                {comment.body}
                {/for}
                    """)
        public static native TemplateInstance issue(IssueModel issue);
    }

    public String discussion(DiscussionModel discussion) {
        return Templates.discussion(discussion).render();
    }

    public String issue(IssueModel issue) {
        return Templates.issue(issue).render();
    }
}
