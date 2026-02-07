package io.quarkiverse.github.pm;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.runtime.Startup;

@ApplicationScoped
public class Github {
    @Inject
    @ConfigProperty(name = "product.manager.github.token")
    String githubToken;

    @Inject
    ObjectMapper objectMapper;

    Client client;
    WebTarget githubApi;
    WebTarget graphqlApi;

    @Startup
    public void startup() {
        client = ClientBuilder.newClient();
        githubApi = client.target("https://api.github.com");
        graphqlApi = client.target("https://api.github.com/graphql");
    }

    public WebTarget graphqlApi() {
        return graphqlApi;
    }

    public WebTarget githubApi() {
        return githubApi;
    }

    public Response graphql(String query) {
        try {
            String json = objectMapper.writeValueAsString(Map.of("query", query));
            return addGithubHeaders(graphqlApi.request()).post(Entity.json(json));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    public Invocation.Builder addGithubHeaders(Invocation.Builder builder) {
        return builder.header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("Authorization", "Bearer " + githubToken);
    }

    public Invocation.Builder from(Link link) {
        return addGithubHeaders(client.invocation(link));
    }

    public Invocation.Builder issues(String repo) {
        return addGithubHeaders(githubApi.path("repos").path(repo).path("issues").request());
    }

    public Invocation.Builder issues(String repo, String since) {
        WebTarget path = issuesPath(repo);
        return addGithubHeaders(path.queryParam("since", since).request());
    }

    public WebTarget issuesPath(String repo) {
        WebTarget path = githubApi.path("repos").path(repo).path("issues");
        return path;
    }
}
