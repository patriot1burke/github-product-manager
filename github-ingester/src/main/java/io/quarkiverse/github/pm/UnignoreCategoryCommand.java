package io.quarkiverse.github.pm;

import java.util.concurrent.Callable;

import jakarta.inject.Inject;

import io.quarkiverse.github.index.GithubIndexService;
import io.quarkiverse.github.index.RepositoryIndex;
import io.quarkiverse.github.pm.util.BaseCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "category", description = "Unignore a category")
public class UnignoreCategoryCommand extends BaseCommand implements Callable<Integer> {
    @Inject
    GithubIndexService index;

    @Parameters(index = "0", description = "Github repo.  i.e. quarkusio/quarkus")
    private String repo;

    @Parameters(index = "1", description = "ID of the category.  do 'ingester show categories' to get the ID if you don't know it.")
    private String category;

    @Override
    public Integer call() throws Exception {
        if (!index.exists(repo.trim())) {
            output.error("Repository [" + repo + "] not found");
            return CommandLine.ExitCode.SOFTWARE;
        }
        RepositoryIndex repoIndex = index.load(repo.trim());
        if (!repoIndex.ignoredCategories.contains(category)) {
            output.warn("Category [" + category + "] not found in ignore list for " + repo);
            return CommandLine.ExitCode.SOFTWARE;
        }
        repoIndex.ignoredCategories.remove(category);
        index.save(repoIndex);
        return CommandLine.ExitCode.OK;
    }

}
