package io.quarkiverse.test;

import java.util.Set;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;

import io.quarkiverse.github.index.PullCacheService;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class PullCacheTest {

    @Inject
    PullCacheService pullCacheService;

    //@Test
    public void testRepos() {
        Set<String> repos = pullCacheService.repos();
        Assertions.assertEquals(1, repos.size());
        Assertions.assertEquals("quarkusio/quarkus", repos.iterator().next());
    }

}
