package io.quarkiverse.github.util;

public interface AppLoggerFactory {
    AppLogger logger(Class name);
}
