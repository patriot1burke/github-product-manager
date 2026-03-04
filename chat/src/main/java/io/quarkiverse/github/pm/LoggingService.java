package io.quarkiverse.github.pm;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkiverse.github.util.AppLogger;
import io.quarkiverse.github.util.AppLoggerFactory;
import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.Startup;

@Singleton
@Unremovable
public class LoggingService {

    @Inject
    ChatContext chatContext;

    @Startup
    public void startup() {
        AppLogger.Factory.instance = new AppLoggerFactory() {
            @Override
            public AppLogger logger(Class clz) {
                return new WebLogger(clz, chatContext);
            }
        };
    }

}
