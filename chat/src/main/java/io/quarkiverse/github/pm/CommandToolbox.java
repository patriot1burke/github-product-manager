package io.quarkiverse.github.pm;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import io.quarkiverse.github.index.model.Earlier;

@ApplicationScoped
public class CommandToolbox {

    @Inject
    ChatContext chatContext;

    // I would like to use IMMEDIATE, but gpt 5.2 only calls one tool at a time.

    @Tool(value = "set the default Github repository") //, returnBehavior = ReturnBehavior.IMMEDIATE)
    public void setDefaultRepository(String repository) {
        chatContext.thinking("Setting default Github repository to " + repository);

    }

    ReturnBehavior returnBehavior = ReturnBehavior.IMMEDIATE;

    @Tool(value = "pull discussions and issues from the current Github repository") //, returnBehavior = ReturnBehavior.IMMEDIATE)
    public void pull(@P(required = false, value = "The repository to pull from") String repository, boolean prune,
            @P(required = false, value = "How long ago should the pull be?") Earlier earlier) {
        chatContext.thinking("Pulling from " + repository);
        chatContext.thinking("Pruning: " + prune);
        chatContext.thinking("Earlier: " + earlier);
    }

}
