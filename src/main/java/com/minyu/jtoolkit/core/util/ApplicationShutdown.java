package com.minyu.jtoolkit.core.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.function.IntConsumer;

@Slf4j
public final class ApplicationShutdown {

    private ApplicationShutdown() {
    }

    public static void exit(ConfigurableApplicationContext context) {
        shutdown(context, System::exit);
    }

    static void shutdown(ConfigurableApplicationContext context, IntConsumer exitAction) {
        try {
            if (context != null) {
                context.close();
            }
        } catch (Exception e) {
            log.warn("Failed to close Spring context during shutdown", e);
        } finally {
            exitAction.accept(0);
        }
    }
}
