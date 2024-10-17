package org.apache.activemq.artemis.scenarios.helpers;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for time related methods
 */
@EqualsAndHashCode()
@Log4j2
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(callSuper = true)
public class TimeHelper {
    /**
     * Wait for a specific amount of time
     *
     * @param delay the amount in milliseconds to wait
     * @throws RuntimeException if interrupted while waiting
     */
    public static void waitFor(long delay) {
        if (delay > 0) {
            try {
                LOGGER.trace("Waiting for {} ms", delay);
                TimeUnit.MILLISECONDS.sleep(delay);
            } catch (InterruptedException e) {
                String errMsg = String.format("Error on sleeping: %s", e.getMessage());
                LOGGER.error(errMsg);
                throw new RuntimeException(errMsg, e);
            }
        } else {
            LOGGER.trace("Delay is < 0, skipping waiting");
        }
    }

    /**
     * Wait for a list of future tasks finish
     *
     * @param id    task id for logging propose
     * @param tasks list of tasks
     */
    public static void waitForTasks(String id, List<Future<?>> tasks) {
        LOGGER.debug("[{}] - Waiting for tasks", id);
        boolean keepChecking = true;
        while (keepChecking) {
            LOGGER.trace("[{}] - All tasks are not done yet", id);
            if (tasks.stream().allMatch(Future::isDone)) {
                LOGGER.trace("[{}] - All tasks are done", id);
                keepChecking = false;
            }
        }
    }
}
