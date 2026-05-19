package io.quarkiverse.ai.github.db;

import java.util.ArrayList;
import java.util.List;

public class Filters {
    public List<String> andLabels = new ArrayList<>();
    public List<String> orLabels = new ArrayList<>();
    public List<String> andFilters = new ArrayList<>();
    public List<String> orFilters = new ArrayList<>();
}
