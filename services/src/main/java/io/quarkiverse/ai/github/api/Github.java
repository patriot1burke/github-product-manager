package io.quarkiverse.ai.github.api;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Link;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkiverse.ai.github.api.GithubAPI.Repository;
import io.quarkiverse.graphql.client.GraphQLClient;
import io.quarkus.runtime.Startup;

@ApplicationScoped
public class Github {
    @Inject
    @ConfigProperty(name = "ai.scanner.github.token")
    String githubToken;

    @Inject
    ObjectMapper objectMapper;

    Client client;
    WebTarget githubApi;

    @Startup
    public void startup() {
        client = ClientBuilder.newClient();
    }

    public Repository repository(String repo) {
        String owner = repo.split("/")[0];
        String name = repo.split("/")[1];
        return api().repository(owner, name);
    }

    @Produces
    @ApplicationScoped
    public GithubAPI api() {
        return new GraphQLClient(objectMapper).query().endpoint("https://api.github.com/graphql").bearer(githubToken)
                .target(GithubAPI.class);
    }

    public WebTarget githubApi() {
        return githubApi;
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
