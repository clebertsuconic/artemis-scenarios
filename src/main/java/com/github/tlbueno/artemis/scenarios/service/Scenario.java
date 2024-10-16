package com.github.tlbueno.artemis.scenarios.service;

import com.github.tlbueno.artemis.scenarios.ResourceManager;
import com.github.tlbueno.artemis.scenarios.helpers.TextHelper;
import com.github.tlbueno.artemis.scenarios.helpers.TimeHelper;
import com.github.tlbueno.artemis.scenarios.model.request.BaseScenarioRequest;
import com.github.tlbueno.artemis.scenarios.model.request.NonTemporaryDestinationScenarioRequest;
import com.github.tlbueno.artemis.scenarios.model.request.app.SourceApp;
import com.github.tlbueno.artemis.scenarios.model.request.app.TargetApp;
import com.github.tlbueno.artemis.scenarios.model.request.endpoint.EndpointBase;
import com.github.tlbueno.artemis.scenarios.model.request.endpoint.EndpointConnection;
import com.github.tlbueno.artemis.scenarios.model.request.endpoint.EndpointSession;
import com.github.tlbueno.artemis.scenarios.model.request.endpoint.consumer.ConsumerEndpoint;
import com.github.tlbueno.artemis.scenarios.model.request.endpoint.consumer.ConsumerEndpointMessage;
import com.github.tlbueno.artemis.scenarios.model.request.endpoint.producer.ProducerEndpoint;
import com.github.tlbueno.artemis.scenarios.model.request.endpoint.producer.ProducerEndpointMessage;
import com.github.tlbueno.artemis.scenarios.model.response.ScenarioResponse;
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
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Locked;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

@EqualsAndHashCode()
@Log4j2
@ToString(callSuper = true)
@Getter
public class Scenario<T extends BaseScenarioRequest> {
    private static final String TIMEOUT_EXCEED_OR_CONSUMER_WAS_CLOSED = "timeout exceed or consumer was closed";

    @NonNull
    private final String id;
    @NonNull
    private final T request;
    @NonNull
    private final ScenarioResponse response;
    private boolean keepRunning;

    @Builder
    public Scenario(@NonNull String id, @NonNull T request, @NonNull ScenarioResponse response) {
        this.keepRunning = true;
        this.id = id;
        this.request = request;
        this.response = response;
    }

    @Locked
    public boolean isKeepRunning() {
        return this.keepRunning;
    }

    @Locked
    public void setKeepRunning(boolean keepRunning) {
        this.keepRunning = keepRunning;
    }

    public void processApps() {
        List<Future<?>> appTasks = new ArrayList<>();
        appTasks.add(ResourceManager.submitTask(this::processTargets));
        appTasks.add(ResourceManager.submitTask(this::processSources));
        LOGGER.info("[{}] - Waiting for all app tasks finish", id);
        TimeHelper.waitForTasks(id, appTasks);
        LOGGER.info("[{}] - All app tasks finished", id);
        setKeepRunning(false);
    }

    public void processSources() {
        List<SourceApp> sources = request.getSources();
        LOGGER.info("[{}] - Processing scenario sources", id);
        List<Future<?>> sourceTasks = new ArrayList<>();
        for (SourceApp source : sources) {
            String sourceId = id + "-" + source.getId();
            if (source.isEnabled()) {
                sourceTasks.add(ResourceManager.submitTask(() -> processEndpoints(sourceId, source.getProducers())));
            } else {
                LOGGER.info("[{}] Scenario source app is disabled, skipping it", sourceId);
            }
        }
        LOGGER.info("[{}] - Waiting for all source tasks finish", id);
        TimeHelper.waitForTasks(id, sourceTasks);
        LOGGER.info("[{}] - All source tasks finished", id);
    }

    public void processTargets() {
        List<TargetApp> targets = request.getTargets();
        LOGGER.info("[{}] - Processing scenario target apps", id);
        List<Future<?>> targetApps = new ArrayList<>();
        for (TargetApp target : targets) {
            String targetId = id + "-" + target.getId();
            if (target.isEnabled()) {
                targetApps.add(ResourceManager.submitTask(() -> processEndpoints(targetId, target.getConsumers())));
            } else {
                LOGGER.info("[{}] Scenario target app is disabled, skipping it", targetId);
            }
        }
        LOGGER.info("[{}] - Waiting for all target tasks finish", id);
        TimeHelper.waitForTasks(id, targetApps);
        LOGGER.info("[{}] - All target tasks finished", id);
    }

    private void consumeMessages(String sessionId, ConsumerEndpoint consumer, Session session) {
        try {
            ConsumerEndpointMessage consumerEndpointMessage = consumer.getMessage();
            String destinationId = id + "-destination";
            String destinationName = null;
            if (request.getClass().equals(NonTemporaryDestinationScenarioRequest.class)) {
                destinationName = ((NonTemporaryDestinationScenarioRequest) request).getDestinationName();
            }
            Destination destination = openDestination(destinationId, sessionId, request.getDestinationClass(), destinationName, session);
            MessageConsumer messageConsumer = session.createConsumer(destination, consumerEndpointMessage.getMsgSelector());
            long commitCounter = 1;
            boolean keepConsuming = true;
            long msgCounter = 1;
            while (isKeepRunning() && keepConsuming) {
                long numOfMsgs = consumerEndpointMessage.getNumOfMsgPerSession();
                if (numOfMsgs >= 0) {
                    if (msgCounter < numOfMsgs) {
                        msgCounter++;
                    } else {
                        keepConsuming = false;
                    }
                }
                Message message;
                long msgTimeout = consumerEndpointMessage.getMsgTimeoutInMs();
                if (msgTimeout <= 0) {
                    LOGGER.trace("[{}] - Trying to consume message without a timeout", sessionId);
                    message = messageConsumer.receive();
                } else {
                    LOGGER.trace("[{}] - Trying to consume message with a timeout of {}", sessionId,
                            msgTimeout);
                    message = messageConsumer.receive(msgTimeout);
                }
                if (message == null) {
                    String errMsg = String.format("[%s] - " + TIMEOUT_EXCEED_OR_CONSUMER_WAS_CLOSED, sessionId);
                    LOGGER.trace(errMsg);
                    throw new RuntimeException(errMsg);
                }
                String messageId = message.getJMSMessageID();
                LOGGER.trace("[{}] - Received message with id {}", sessionId, messageId);
                if (consumer.getSession().isTransactedSession()) {
                    commitCounter = evaluateCommitOnEveryNMsg(sessionId, session, commitCounter, consumerEndpointMessage.getCommitOnEveryXMsgs());
                } else {
                    LOGGER.trace("[{}] - Message with id {} received but not commit yet", sessionId,
                            messageId);
                }
                // TODO: Implement a replier
                    /*
                    if (replierConsumer && consumerConfig.isSendReply()) {
                        ReplierProcessorMessage processorMessage = ReplierProcessorMessage.builder()
                                .withCorrelationId(message.getJMSCorrelationID())
                                .withReplyDestination(message.getJMSReplyTo().toString())
                                .build();
                        Replier.putToProcessorQueue(processorMessage);
                    }
                    */
                long delayBetweenMsgs = consumerEndpointMessage.getDelayBetweenMsgs();
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
    }

    private JmsPoolConnectionFactory createConnectionPool(String id, EndpointConnection connection) {
        LOGGER.debug("[{}] Creating connection pool", id);
        int numOfConnections = connection.getNumOfConnections();
        int numOfSessionsPerConnection = connection.getNumOfSessionsPerConnection();
        JmsConnectionFactory connectionFactory = getJmsConnectionFactory(id, connection);
        return ResourceManager.getJmsPoolConnectionFactory(id, numOfConnections, numOfSessionsPerConnection,
                connectionFactory);
    }

    private List<Session> createSessions(String connectionId, Connection connection, int numOfSessionsPerConnection,
                                         EndpointSession session) {
        LOGGER.debug("[{}] Creating connection sessions", connectionId);
        List<Session> sessions = new ArrayList<>();
        for (int i = 0; i < numOfSessionsPerConnection; i++) {
            String sessionId = connectionId + "-session-" + i;
            LOGGER.debug("[{}] Creating session", sessionId);
            boolean isTransactedSession = session.isTransactedSession();
            int sessionAckMode = session.getSessionAckMode();
            try {
                sessions.add(connection.createSession(isTransactedSession, sessionAckMode));
            } catch (JMSException e) {
                String errMsg = String.format("[%s] - Error on creating session: %s", sessionId, e.getMessage());
                LOGGER.error(errMsg);
                throw new RuntimeException(errMsg, e);
            }
        }
        return sessions;
    }

    private long evaluateCommitOnEveryNMsg(String sessionId, Session session, long commitCounter, Integer commitOnEveryXMsgs) {
        LOGGER.trace("[{}] - Evaluating commit on every N messages with commit counter: {} and commit on: {}",
                sessionId, commitCounter, commitOnEveryXMsgs);
        long counter;
        if (commitCounter >= commitOnEveryXMsgs) {
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
                    commitCounter, commitOnEveryXMsgs);
            counter = commitCounter + 1;
        }
        return counter;
    }

    private JmsConnectionFactory getJmsConnectionFactory(String id, EndpointConnection connection) {
        String username = connection.getUsername();
        LOGGER.debug("[{}] Using connection username {}", id, username);
        String password = connection.getPassword();
        LOGGER.debug("[{}] Using connection password {}", id, password);
        boolean hasUsername = username != null && !username.isEmpty() && !username.isBlank();
        boolean hasPassword = password != null && !password.isEmpty() && !password.isBlank();
        String url = connection.getUrl();
        LOGGER.debug("[{}] Using connection url {}", id, url);
        JmsConnectionFactory connectionFactory;
        if (hasUsername || hasPassword) {
            LOGGER.debug("[{}] Creating connection factory with username and password", id);
            connectionFactory = new JmsConnectionFactory(username, password, url);
        } else {
            LOGGER.debug("[{}] Creating anonymous connection factory", id);
            connectionFactory = new JmsConnectionFactory(url);
        }
        return connectionFactory;
    }

    private Destination openDestination(String destinationId, String sessionId, String destinationClassName,
                                        String destinationName, Session session) {
        try {
            Class<?> destinationClass = Class.forName(destinationClassName);
            Destination destination;
            if (Queue.class.isAssignableFrom(destinationClass)) {
                if (TemporaryQueue.class.equals(destinationClass)) {
                    LOGGER.debug("[{}] - Creating a temporary queue destination", sessionId);
                    destination = ResourceManager.createTemporaryDestination(sessionId, destinationId, session);
                } else {
                    LOGGER.trace("[{}] - Creating a queue destination with name {}", sessionId,
                            destinationName);
                    destination = session.createQueue(destinationName);
                }
            } else if (Topic.class.isAssignableFrom(destinationClass)) {
                // TODO: Create temporary topic
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

    private void populateMsgProperties(String sessionId, TextMessage message, Map<String, String> msgProperties) {
        if (msgProperties == null) {
            LOGGER.trace("[{}] - Not populating message properties as message properties is null", sessionId);
        } else {
            LOGGER.trace("[{}] - Populating message properties", sessionId);
            msgProperties.forEach((name, value) -> {
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

    private void processEndpoints(String appId, List<? extends EndpointBase> endpoints) {
        LOGGER.info("[{}] Processing endpoints", appId);
        List<Future<?>> endpointTasks = new ArrayList<>();
        for (EndpointBase endpoint : endpoints) {
            String endpointId = appId + "-" + endpoint.getId();
            if (endpoint.isEnabled()) {
                LOGGER.debug("[{}] Processing endpoint", endpointId);
                endpointTasks.add(ResourceManager.submitTask(() -> runEndpoint(endpointId, endpoint)));
            } else {
                LOGGER.info("[{}] Consumer is disabled, skipping it", endpointId);
            }
        }
        LOGGER.info("[{}] - Waiting for all endpoint connection tasks finish", appId);
        TimeHelper.waitForTasks(appId, endpointTasks);
        LOGGER.info("[{}] - All endpoint connection tasks finished", appId);
    }

    private void runEndpoint(String endpointId, EndpointBase endpoint) {
        LOGGER.info("[{}] - Running endpoint", endpointId);
        String poolId = endpointId + "-pool";
        JmsPoolConnectionFactory pool = createConnectionPool(poolId, endpoint.getConnection());
        int numOfSessionsPerConnection = pool.getMaxSessionsPerConnection();
        List<Future<?>> connectionTasks = new ArrayList<>();
        for (int i = 1; i <= pool.getMaxConnections(); i++) {
            int connectionNum = i;
            connectionTasks.add(ResourceManager.submitTask(() -> {
                try {
                    String connectionId = endpointId + "-connection-" + connectionNum;
                    LOGGER.debug("[{}] Creating connection", connectionId);
                    Connection connection = pool.createConnection();
                    connection.start();
                    List<Session> sessions = createSessions(connectionId, connection, numOfSessionsPerConnection,
                            endpoint.getSession());
                    List<Future<?>> sessionTasks = new ArrayList<>();
                    int sessionNum = 1;
                    for (Session session : sessions) {
                        String sessionId = connectionId + "-session-" + sessionNum;
                        if (endpoint.getClass().equals(ConsumerEndpoint.class)) {
                            Future<?> consumerTask = ResourceManager.submitTask(() -> consumeMessages(sessionId,
                                    (ConsumerEndpoint) endpoint, session));
                            sessionTasks.add(consumerTask);
                        } else if (endpoint.getClass().equals(ProducerEndpoint.class)) {
                            Future<?> producerTask = ResourceManager.submitTask(() -> sendMessages(sessionId,
                                    (ProducerEndpoint) endpoint, session));
                            sessionTasks.add(producerTask);
                        }
                        sessionNum++;
                    }
                    LOGGER.info("[{}] - Waiting for all endpoint connection tasks finish", connectionId);
                    TimeHelper.waitForTasks(connectionId, sessionTasks);
                    LOGGER.info("[{}] - All endpoint connection tasks finished", connectionId);
                    connection.stop();
                } catch (JMSException e) {
                    String errMsg = String.format("[%s] - Error on creating connection: %s", endpointId, e.getMessage());
                    LOGGER.error(errMsg);
                    throw new RuntimeException(errMsg, e);
                }
            }));
        }
        LOGGER.info("[{}] - Waiting for all endpoints tasks finish", endpointId);
        TimeHelper.waitForTasks(endpointId, connectionTasks);
        LOGGER.info("[{}] - All consumer endpoints tasks finished", endpointId);
        pool.stop();
        ResourceManager.removeJmsPoolConnectionFactory(poolId);
    }

    private void sendMessages(String sessionId, ProducerEndpoint producer, Session session) {
        try {
            ProducerEndpointMessage producerEndpointMessage = producer.getMessage();
            String destinationId = id + "-destination";
            String destinationName = null;
            String replyDestinationName = null;
            if (request.getClass().equals(NonTemporaryDestinationScenarioRequest.class)) {
                destinationName = ((NonTemporaryDestinationScenarioRequest) request).getDestinationName();
                replyDestinationName = ((NonTemporaryDestinationScenarioRequest) request).getReplyDestinationName();
            }
            Destination destination = openDestination(destinationId, sessionId, request.getDestinationClass(),
                    destinationName, session);
            MessageProducer messageProducer = session.createProducer(destination);
            long commitCounter = 1;
            boolean keepProducing = true;
            long msgCounter = 1;
            while (keepProducing) {
                long numOfMsgs = producerEndpointMessage.getNumOfMsgPerSession();
                if (numOfMsgs >= 0) {
                    if (msgCounter < numOfMsgs) {
                        msgCounter++;
                    } else {
                        keepProducing = false;
                    }
                }
                String randomText = TextHelper.generateRandomMsgText(producerEndpointMessage.getMsgSizeInKb());
                TextMessage message = session.createTextMessage(randomText);
                message.setJMSDeliveryMode(producerEndpointMessage.getDeliveryMode());
                message.setJMSCorrelationID(ResourceManager.generateUniqueUUID().toString());
                if (producerEndpointMessage.isSetReplyDestination()) {
                    String replyDestinationId = id + "-reply-destination";
                    Destination replyDestination = openDestination(replyDestinationId, sessionId,
                            request.getDestinationClass(), replyDestinationName, session);
                    message.setJMSReplyTo(replyDestination);
                }
                populateMsgProperties(sessionId, message, producerEndpointMessage.getMsgProperties());
                messageProducer.send(message);
                String messageId = message.getJMSMessageID();
                LOGGER.trace("[{}] - Sent message with id {}", sessionId, messageId);
                if (producer.getSession().isTransactedSession()) {
                    commitCounter = evaluateCommitOnEveryNMsg(sessionId, session, commitCounter,
                            producerEndpointMessage.getCommitOnEveryXMsgs());
                } else {
                    LOGGER.trace("[{}] - Message with id {} sent but not commit yet", sessionId, messageId);
                }
                long delayBetweenMsgs = producerEndpointMessage.getDelayBetweenMsgs();
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
    }
}
