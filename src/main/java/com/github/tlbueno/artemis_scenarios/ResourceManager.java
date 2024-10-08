package com.github.tlbueno.artemis_scenarios;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Session;
import jakarta.jms.TemporaryQueue;
import lombok.EqualsAndHashCode;
import lombok.Locked;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@ApplicationScoped
@EqualsAndHashCode()
@Log4j2
@ToString(callSuper = true)
public class ResourceManager {
    private static final ConcurrentHashMap<String, JmsPoolConnectionFactory> jmsPool = new ConcurrentHashMap<>();
    private static final ExecutorService services = Executors.newCachedThreadPool();
    private static final ConcurrentHashMap<String, String> temporaryDestinationName = new ConcurrentHashMap<>();

    @Locked
    public static Destination createTemporaryDestination(String sessionId, String queueId, Session session) {
        LOGGER.debug("[{}] - Setting temporary destination name", sessionId);
        String destinationName = temporaryDestinationName.get(queueId);
        Destination destination;
        try {
            if (destinationName == null || destinationName.isEmpty() || destinationName.isBlank()) {
                destination = session.createTemporaryQueue();
                destinationName = ((TemporaryQueue) destination).getQueueName();
                temporaryDestinationName.put(queueId, destinationName);
                LOGGER.debug("[{}] - Temporary queue destination created: {}", sessionId, destinationName);

            } else {
                LOGGER.debug("[{}] - Temporary queue destination {} already created, skipping creation", sessionId,
                        destinationName);
                destination = session.createQueue(destinationName);
            }
        } catch (JMSException e) {
            String errMsg = String.format("[%s] - Failed on create JMS destination: %s", sessionId, e.getMessage());
            LOGGER.error(errMsg);
            throw new RuntimeException(errMsg, e);
        }
        return destination;
    }

    @Locked
    public static JmsPoolConnectionFactory getJmsPoolConnectionFactory(String id, int numOfConnections,
                                                                       int numOfSessionsPerConnection,
                                                                       ConnectionFactory connectionFactory) {
        LOGGER.debug("[{}] - About to create JMS connection pool", id);
        JmsPoolConnectionFactory pool;
        if (jmsPool.containsKey(id)) {
            LOGGER.debug("[{}] - JMS connection pool already exist, skipping creation", id);
            pool = jmsPool.get(id);
        } else {
            LOGGER.debug("[{}] - Creating JMS connection pool", id);
            pool = new JmsPoolConnectionFactory();
            LOGGER.debug("[{}] - Set number of connections to {}", id, numOfConnections);
            pool.setMaxConnections(numOfConnections);
            LOGGER.debug("[{}] - Set number of sessions per connection to {}", id,
                    numOfSessionsPerConnection);
            pool.setMaxSessionsPerConnection(numOfSessionsPerConnection);
            pool.setConnectionFactory(connectionFactory);
            jmsPool.put(id, pool);
        }
        return pool;
    }

    @Locked
    public static void removeJmsPoolConnectionFactory(String id) {
        LOGGER.debug("[{}] - About to remove JMS connection pool", id);
        jmsPool.remove(id);
    }

    public static Future<?> submitTask(Runnable task) {
        LOGGER.trace("Submitting task");
        return services.submit(task);
    }

    public static void waitForTasks(List<Future<?>> tasks) {
        LOGGER.debug("Waiting for tasks");
        boolean keepRunning = true;
        while (keepRunning) {
            LOGGER.trace("All tasks are not done yet");
            if (tasks.stream().allMatch(Future::isDone)) {
                LOGGER.trace("All tasks are done");
                keepRunning = false;
            }
        }
    }
}
