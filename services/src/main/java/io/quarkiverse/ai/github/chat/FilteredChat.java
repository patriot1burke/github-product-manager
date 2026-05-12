package io.quarkiverse.ai.github.chat;

import jakarta.inject.Inject;

import dev.langchain4j.service.UserMessage;
import io.quarkiverse.ai.github.db.RepositoryFilter;
import io.quarkiverse.langchain4j.chatscopes.ChatRoute;
import io.quarkiverse.langchain4j.chatscopes.ChatRouteContext;
import io.quarkiverse.langchain4j.chatscopes.ChatScope;
import io.quarkiverse.langchain4j.chatscopes.ChatScoped;

@ChatScoped
public class FilteredChat {

    RepositoryFilter filter;
    @Inject
    ChatRouteContext ctx;

    public void setFilter(RepositoryFilter filter) {
        this.filter = filter;
    }

    @ChatRoute("filtered-chat")
    public void chat(@UserMessage String msg) {
        if (msg.equals("finished")) {
            ChatScope.pop();
            return;
        }

    }
}
