package org.apache.activemq.artemis.scenarios.model.request.endpoint;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;

@EqualsAndHashCode()
@Log4j2
@SuperBuilder(setterPrefix = "with")
@ToString(callSuper = true)
@Getter
public abstract class BaseEndpointMessage {
    @NonNull
    private Integer commitOnEveryXMsgs;
    @NonNull
    private Integer delayBetweenMsgs;
    @NonNull
    private Integer numOfMsgPerSession;
}
