package com.github.tlbueno.artemis_scenarios.resources.scenario;

import com.github.tlbueno.artemis_scenarios.ResourceManager;
import com.github.tlbueno.artemis_scenarios.helpers.TextHelper;
import com.github.tlbueno.artemis_scenarios.helpers.TimeHelper;
import com.github.tlbueno.artemis_scenarios.models.scenario.ScenarioConnection;
import com.github.tlbueno.artemis_scenarios.models.scenario.ScenarioConsumer;
import com.github.tlbueno.artemis_scenarios.models.scenario.ScenarioConsumerMessage;
import com.github.tlbueno.artemis_scenarios.models.scenario.ScenarioProducer;
import com.github.tlbueno.artemis_scenarios.models.scenario.ScenarioProducerMessage;
import com.github.tlbueno.artemis_scenarios.models.scenario.ScenarioRequest;
import com.github.tlbueno.artemis_scenarios.models.scenario.ScenarioResponse;
import com.github.tlbueno.artemis_scenarios.models.scenario.ScenarioSession;
import com.github.tlbueno.artemis_scenarios.models.scenario.ScenarioSource;
import com.github.tlbueno.artemis_scenarios.models.scenario.ScenarioTarget;
import jakarta.jms.Connection;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TemporaryQueue;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

@EqualsAndHashCode()
@Log4j2
@Path("/scenario")
@ToString(callSuper = true)
public class ScenarioResource {

    private static final String TIMEOUT_EXCEED_OR_CONSUMER_WAS_CLOSED = "timeout exceed or consumer was closed";
    private ScenarioRequest request;

    @POST
    @Consumes("application/yaml")
    @Produces("application/yaml")
    public Response processScenarioRequest(ScenarioRequest request) {
        this.request = request;
        ScenarioResponse scenarioResponse = new ScenarioResponse();
        String scenarioId = request.getId();
        scenarioResponse.setId(scenarioId);
        if (request.isEnabled()) {
            LOGGER.info("[{}] - Processing scenario request", scenarioId);
            LOGGER.debug("{}", request.toString());
            processTargets(request.getTargets());
            processSources(scenarioId, request.getSources());
            scenarioResponse.setResult("submitted");
        } else {
            scenarioResponse.setResult("scenario disabled");
        }
        return Response.ok(scenarioResponse).build();
    }

    private void consume(String consumerId, ScenarioConsumer consumer) {
        LOGGER.info("[{}] - Processing consumer", consumerId);
        ResourceManager.submitTask(() -> {
            String poolId = consumerId + UUID.randomUUID();
            JmsPoolConnectionFactory pool = createConnectionPool(poolId, consumer.getConnection());
            int numOfSessionsPerConnection = pool.getMaxSessionsPerConnection();
            List<Future<?>> connectionTasks = new ArrayList<>();
            LOGGER.debug("[{}] About to process connections", consumerId);
            for (int i = 1; i <= pool.getMaxConnections(); i++) {
                int connectionNum = i;
                connectionTasks.add(ResourceManager.submitTask(() -> {
                    try {
                        String connectionId = consumerId + "-" + connectionNum;
                        LOGGER.debug("[{}] Creating connection", connectionId);
                        Connection connection = pool.createConnection();
                        connection.start();
                        LOGGER.debug("[{}] About to process sessions", connectionId);
                        List<Session> sessions = processSession(connectionId, connection, numOfSessionsPerConnection,
                                consumer.getSession());
                        List<Future<?>> sendMessagesTasks = new ArrayList<>();
                        int sessionNum = 1;
                        for (Session session : sessions) {
                            String sessionId = connectionId + "-" + sessionNum;
                            LOGGER.debug("[{}] About to send messages for session #{}", consumerId, sessionId);
                            sendMessagesTasks.add(consumeMessages(sessionId, consumer, session));
                            sessionNum++;
                        }
                        LOGGER.info("[{}] - Waiting for all message tasks finish", connectionId);
                        ResourceManager.waitForTasks(sendMessagesTasks);
                        LOGGER.info("[{}] - All message tasks finished", connectionId);
                        connection.stop();
                    } catch (JMSException e) {
                        throw new RuntimeException(e);
                    }
                }));
            }
            LOGGER.info("[{}] - Waiting for all connection tasks finish", consumerId);
            ResourceManager.waitForTasks(connectionTasks);
            LOGGER.info("[{}] - All connection tasks finished", consumerId);
            pool.stop();
            ResourceManager.removeJmsPoolConnectionFactory(poolId);
        });
    }

    private Future<?> consumeMessages(String sessionId, ScenarioConsumer consumer, Session session) {
        return ResourceManager.submitTask(() -> {
            try {
                ScenarioConsumerMessage scenarioMessage = consumer.getMessage();
                String queueId = request.getId() + "-dst";
                Destination destination = openDestination(queueId, sessionId, request.getDestinationClass(),
                        request.getDestinationName(), session);
                MessageConsumer msgConsumer = session.createConsumer(destination, scenarioMessage.getMsgSelector());
                long commitCounter = 1;
                boolean keepConsuming = true;
                long msgCounter = 1;
                while (keepConsuming) {
                    long numOfMsgs = scenarioMessage.getNumOfMsgPerSession();
                    if (numOfMsgs >= 0) {
                        if (msgCounter < numOfMsgs) {
                            msgCounter++;
                        } else {
                            keepConsuming = false;
                        }
                    }
                    Message message;
                    long msgTimeout = scenarioMessage.getMsgTimeoutInMs();
                    if (msgTimeout <= 0) {
                        LOGGER.trace("[{}] - Trying to consume message without a timeout", sessionId);
                        message = msgConsumer.receive();
                    } else {
                        LOGGER.trace("[{}] - Trying to consume message with a timeout of {}", sessionId,
                                msgTimeout);
                        message = msgConsumer.receive(msgTimeout);
                    }
                    if (message == null) {
                        String errMsg = String.format("[%s] - " + TIMEOUT_EXCEED_OR_CONSUMER_WAS_CLOSED, sessionId);
                        LOGGER.trace(errMsg);
                        throw new RuntimeException(errMsg);
                    }
                    String messageId = message.getJMSMessageID();
                    LOGGER.trace("[{}] - Received message with id {}", sessionId, messageId);
                    if (consumer.getSession().isTransactedSession()) {
                        commitCounter = evaluateCommitOnEveryNMsg(sessionId, session, commitCounter, scenarioMessage.getCommitOnEveryXMsgs());
                    } else {
                        LOGGER.trace("[{}] - Message with id {} received but not commit yet", sessionId,
                                messageId);
                    }
                    /*if (replierConsumer && consumerConfig.isSendReply()) {
                        ReplierProcessorMessage processorMessage = ReplierProcessorMessage.builder()
                                .withCorrelationId(message.getJMSCorrelationID())
                                .withReplyDestination(message.getJMSReplyTo().toString())
                                .build();
                        Replier.putToProcessorQueue(processorMessage);
                    }*/
                    long delayBetweenMsgs = scenarioMessage.getDelayBetweenMsgs();
                    if (delayBetweenMsgs > 0) {
                        TimeHelper.waitFor(delayBetweenMsgs);
                    }
                }
                session.close();
            } catch (JMSException | RuntimeException e) {
                String errMsg = String.format("[%s] - Error on consuming message: %s", sessionId, e.getMessage());
                LOGGER.error(errMsg);
                throw new RuntimeException(errMsg, e);
            }
        });
    }

    private JmsPoolConnectionFactory createConnectionPool(String producerId, ScenarioConnection connection) {
        LOGGER.debug("[{}] Creating connection pool", producerId);
        int numOfConnections = connection.getNumOfConnections();
        int numOfSessionsPerConnection = connection.getNumOfSessionsPerConnection();
        JmsConnectionFactory connectionFactory = getJmsConnectionFactory(producerId, connection);
        return ResourceManager.getJmsPoolConnectionFactory(producerId, numOfConnections, numOfSessionsPerConnection,
                connectionFactory);
    }

    private long evaluateCommitOnEveryNMsg(String sessionId, Session session, long commitCounter, long commitOn) {
        LOGGER.trace("[{}] - Evaluating commit on every N messages with commit counter: {} and commit on: {}",
                sessionId, commitCounter, commitOn);
        long counter;
        if (commitCounter >= commitOn) {
            try {
                LOGGER.trace("[{}] - Committing", sessionId);
                session.commit();
            } catch (JMSException e) {
                String errMsg = String.format("[%s] - Failed on session commit: %s", sessionId, e.getMessage());
                LOGGER.error(errMsg);
                throw new RuntimeException(errMsg, e);
            }
            counter = 1;
        } else {
            LOGGER.trace("[ {}] - Not committing yet as commit counter {} < commit on {}", sessionId,
                    commitCounter, commitOn);
            counter = commitCounter + 1;
        }
        return counter;
    }

    private JmsConnectionFactory getJmsConnectionFactory(String producerId, ScenarioConnection connection) {
        LOGGER.debug("[{}] About to create connection factory", producerId);
        String username = connection.getUsername();
        LOGGER.debug("[{}] Using connection username {}", producerId, username);
        String password = connection.getPassword();
        LOGGER.debug("[{}] Using connection password {}", producerId, password);
        boolean hasUsername = username != null && !username.isEmpty() && !username.isBlank();
        boolean hasPassword = password != null && !password.isEmpty() && !password.isBlank();
        String url = connection.getUrl();
        LOGGER.debug("[{}] Using connection url {}", producerId, url);
        JmsConnectionFactory connectionFactory;
        if (hasUsername || hasPassword) {
            LOGGER.debug("[{}] Creating connection factory with username and password", producerId);
            connectionFactory = new JmsConnectionFactory(username, password, url);
        } else {
            LOGGER.debug("[{}] Creating anonymous connection factory", producerId);
            connectionFactory = new JmsConnectionFactory(url);
        }
        return connectionFactory;
    }

    private Destination openDestination(String queueId, String sessionId, String destinationClassName,
                                        String destinationName, Session session) {
        try {
            Class<?> destinationClass = Class.forName(destinationClassName);
            Destination destination;
            if (Queue.class.isAssignableFrom(destinationClass)) {
                if (TemporaryQueue.class.equals(destinationClass)) {
                    LOGGER.debug("[{}] - Creating a temporary queue destination", sessionId);
                    destination = ResourceManager.createTemporaryDestination(sessionId, queueId, session);
                } else {
                    LOGGER.trace("[{}] - Creating a queue destination with name {}", sessionId,
                            destinationName);
                    destination = session.createQueue(destinationName);
                }
            } else if (Topic.class.isAssignableFrom(destinationClass)) {
                LOGGER.debug("[{}] - Creating a topic destination with name {}", sessionId, destinationName);
                destination = session.createTopic(destinationName);
            } else {
                String errMsg = String.format("[%s] - Error: tried to create unsupported JMS destination", sessionId);
                LOGGER.error(errMsg);
                throw new RuntimeException(errMsg);
            }
            return destination;
        } catch (ClassNotFoundException | JMSException e) {
            String errMsg = String.format("[%s] - Error on open destination: %s", sessionId, e.getMessage());
            LOGGER.error(errMsg);
            throw new RuntimeException(errMsg, e);
        }

    }

    private void populateMsgProperties(String sessionId, Message message, Map<String, String> msgsProperties) {
        if (msgsProperties == null) {
            LOGGER.trace("[{}] - Not populating message properties as message properties is null", sessionId);
        } else {
            LOGGER.trace("[{}] - Populating message properties", sessionId);
            msgsProperties.forEach((name, value) -> {
                boolean hasPropertyName = name != null && !name.isEmpty() && !name.isBlank();
                boolean hasPropertyValue = value != null && !value.isEmpty() && !value.isBlank();
                if (hasPropertyName && hasPropertyValue) {
                    try {
                        LOGGER.trace("[{}] - Populating message with property name {} and value {}", sessionId,
                                name, value);
                        message.setStringProperty(name, value);
                    } catch (JMSException e) {
                        String errMsg = String.format("[%s] - Error on setting message property: %s", sessionId,
                                e.getMessage());
                        LOGGER.error(errMsg);
                        throw new RuntimeException(errMsg, e);
                    }
                } else {
                    LOGGER.trace("[{}] - Not populating message with property as name or value is null",
                            sessionId);
                }
            });
        }
    }

    private void processConsumers(String targetId, List<ScenarioConsumer> consumers) {
        LOGGER.debug("[{}] Processing target producers", targetId);
        for (ScenarioConsumer consumer : consumers) {
            String consumerId = targetId + "-" + consumer.getId();
            if (consumer.isEnabled()) {
                LOGGER.debug("[{}] Processing consumer", consumerId);
                consume(consumerId, consumer);
            } else {
                LOGGER.info("[{}] Consumer is disabled, skipping it", consumerId);
            }
        }
    }

    private void processProducer(String sourceId, List<ScenarioProducer> producers) {
        LOGGER.debug("[{}] Processing source producers", sourceId);
        for (ScenarioProducer producer : producers) {
            String producerId = sourceId + "-" + producer.getId();
            if (producer.isEnabled()) {
                LOGGER.debug("[{}] Processing producer", producerId);
                produce(producerId, producer);
            } else {
                LOGGER.info("[{}] Producer is disabled, skipping it", producerId);
            }
        }
    }

    private List<Session> processSession(String connectionId, Connection connection, int numOfSessionsPerConnection,
                                         ScenarioSession session) {
        List<Session> sessions = new ArrayList<>();
        for (int i = 0; i < numOfSessionsPerConnection; i++) {
            String sessionId = connectionId + "-" + i;
            LOGGER.debug("[{}] Processing sessions", sessionId);
            boolean isTransactedSession = session.isTransactedSession();
            int sessionAckMode = session.getSessionAckMode();
            try {
                sessions.add(connection.createSession(isTransactedSession, sessionAckMode));
            } catch (JMSException e) {
                throw new RuntimeException(e);
            }
        }
        return sessions;
    }

    private void processSources(String scenarioId, List<ScenarioSource> sources) {
        LOGGER.info("Processing scenario sources");
        for (ScenarioSource source : sources) {
            String sourceId = scenarioId + "-" + source.getId();
            if (source.isEnabled()) {
                LOGGER.debug("[{}] About to process producers", sourceId);
                processProducer(sourceId, source.getProducers());
            } else {
                LOGGER.info("[{}] Source is disabled, skipping it", sourceId);
            }
        }
    }

    private void processTargets(List<ScenarioTarget> targets) {
        LOGGER.info("Processing scenario targets");
        for (ScenarioTarget target : targets) {
            String targetId = target.getId();
            if (target.isEnabled()) {
                LOGGER.debug("[{}] About to process consumers", targetId);
                processConsumers(targetId, target.getConsumers());
            } else {
                LOGGER.info("[{}] Target is disabled, skipping it", targetId);
            }
        }
    }

    private void produce(String producerId, ScenarioProducer producer) {
        LOGGER.info("[{}] - Processing producer", producerId);
        ResourceManager.submitTask(() -> {
            String poolId = producerId + UUID.randomUUID();
            JmsPoolConnectionFactory pool = createConnectionPool(poolId, producer.getConnection());
            int numOfSessionsPerConnection = pool.getMaxSessionsPerConnection();
            List<Future<?>> connectionTasks = new ArrayList<>();
            LOGGER.debug("[{}] About to process connections", producerId);
            for (int i = 1; i <= pool.getMaxConnections(); i++) {
                int connectionNum = i;
                connectionTasks.add(ResourceManager.submitTask(() -> {
                    try {
                        String connectionId = producerId + "-" + connectionNum;
                        LOGGER.debug("[{}] Creating connection", connectionId);
                        Connection connection = pool.createConnection();
                        connection.start();
                        LOGGER.debug("[{}] About to process sessions", connectionId);
                        List<Session> sessions = processSession(connectionId, connection, numOfSessionsPerConnection,
                                producer.getSession());
                        List<Future<?>> sendMessagesTasks = new ArrayList<>();
                        int sessionNum = 1;
                        for (Session session : sessions) {
                            String sessionId = connectionId + "-" + sessionNum;
                            LOGGER.debug("[{}] About to send messages for session #{}", producerId, sessionId);
                            sendMessagesTasks.add(sendMessages(sessionId, producer, session));
                            sessionNum++;
                        }
                        LOGGER.info("[{}] - Waiting for all message tasks finish", connectionId);
                        ResourceManager.waitForTasks(sendMessagesTasks);
                        LOGGER.info("[{}] - All message tasks finished", connectionId);
                        connection.stop();
                    } catch (JMSException e) {
                        throw new RuntimeException(e);
                    }
                }));
            }
            LOGGER.info("[{}] - Waiting for all connection tasks finish", producerId);
            ResourceManager.waitForTasks(connectionTasks);
            LOGGER.info("[{}] - All connection tasks finished", producerId);
            pool.stop();
            ResourceManager.removeJmsPoolConnectionFactory(poolId);
        });
    }

    private Future<?> sendMessages(String sessionId, ScenarioProducer producer, Session session) {
        return ResourceManager.submitTask(() -> {
            try {
                ScenarioProducerMessage scenarioMessage = producer.getMessage();
                String destinationId = request.getId() + "-destination";
                Destination destination = openDestination(destinationId, sessionId, request.getDestinationClass(),
                        request.getDestinationName(), session);
                MessageProducer msgProducer = session.createProducer(destination);
                long commitCounter = 1;
                boolean keepProducing = true;
                long msgCounter = 1;
                while (keepProducing) {
                    long numOfMsgs = scenarioMessage.getNumOfMsgPerSession();
                    if (numOfMsgs >= 0) {
                        if (msgCounter < numOfMsgs) {
                            msgCounter++;
                        } else {
                            keepProducing = false;
                        }
                    }
                    String randomText = TextHelper.generateRandomMsgText(scenarioMessage.getMsgSizeInKb());
                    TextMessage message = session.createTextMessage(randomText);
                    message.setJMSDeliveryMode(scenarioMessage.getDeliveryMode());
                    message.setJMSCorrelationID(UUID.randomUUID().toString());
                    if (scenarioMessage.isSetReplyDestination()) {
                        String replyDestinationId = request.getId() + "-reply-destination";
                        Destination replyDestination = openDestination(replyDestinationId, sessionId, request.getDestinationClass(),
                                request.getDestinationName(), session);
                        message.setJMSReplyTo(replyDestination);
                    }
                    populateMsgProperties(sessionId, message, scenarioMessage.getMsgProperties());
                    msgProducer.send(message);
                    String messageId = message.getJMSMessageID();
                    LOGGER.trace("[{}] - Sent message with id {}", sessionId, messageId);
                    if (producer.getSession().isTransactedSession()) {
                        commitCounter = evaluateCommitOnEveryNMsg(sessionId, session, commitCounter,
                                scenarioMessage.getCommitOnEveryXMsgs());
                    } else {
                        LOGGER.trace("[{}] - Message with id {} sent but not commit yet", sessionId, messageId);
                    }
                    long delayBetweenMsgs = scenarioMessage.getDelayBetweenMsgs();
                    if (delayBetweenMsgs > 0) {
                        TimeHelper.waitFor(delayBetweenMsgs);
                    }
                }
                session.close();
            } catch (JMSException e) {
                String errMsg = String.format("[%s] - Error on sending message: %s", sessionId, e.getMessage());
                LOGGER.error(errMsg);
                throw new RuntimeException(errMsg, e);
            }
        });
    }
}
