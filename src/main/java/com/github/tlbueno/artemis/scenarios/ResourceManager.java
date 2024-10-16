package com.github.tlbueno.artemis.scenarios;

import com.github.tlbueno.artemis.scenarios.model.request.BaseScenarioRequest;
import com.github.tlbueno.artemis.scenarios.service.Scenario;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@EqualsAndHashCode()
@Log4j2
@ToString(callSuper = true)
public class ResourceManager {
    private static final Set<UUID> generatedUUIDs = new HashSet<>();
    private static final ConcurrentHashMap<String, JmsPoolConnectionFactory> jmsPool = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Scenario<? extends BaseScenarioRequest>> scenarioMap = new ConcurrentHashMap<>();
    private static final ExecutorService services = Executors.newCachedThreadPool();
    private static final ConcurrentHashMap<String, String> temporaryDestinationName = new ConcurrentHashMap<>();

    @Locked
    public static void addScenario(String id, Scenario<? extends BaseScenarioRequest> scenario) {
        scenarioMap.put(id, scenario);
    }

    @Locked
    public static Destination createTemporaryDestination(String sessionId, String queueId, Session session) {
        LOGGER.debug("[{}] - Setting temporary destination name", sessionId);
        String destinationName = temporaryDestinationName.get(queueId);
        Destination destination;
        try {
            // TODO: create temporary topic
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
    public static UUID generateUniqueUUID() {
        UUID newUUID;
        do {
            newUUID = UUID.randomUUID();
        } while (!generatedUUIDs.add(newUUID)); // Keep generating until a unique UUID is added to the set
        return newUUID;
    }

    @Locked
    public static JmsPoolConnectionFactory getJmsPoolConnectionFactory(String id, int numOfConnections,
                                                                       int numOfSessionsPerConnection,
                                                                       ConnectionFactory connectionFactory) {
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
    public static Scenario<? extends BaseScenarioRequest> getScenario(String id) {
        return scenarioMap.get(id);
    }

    @Locked
    public static List<Scenario<? extends BaseScenarioRequest>> getScenarios() {
        return scenarioMap.values().stream().toList();
    }

    @Locked
    public static void removeJmsPoolConnectionFactory(String id) {
        LOGGER.debug("[{}] - Removing to remove JMS connection pool", id);
        jmsPool.remove(id);
    }

    @Locked
    public static void removeScenario(String id) {
        scenarioMap.remove(id);
    }

    public static Future<?> submitTask(Runnable task) {
        return services.submit(task);
    }

}
