package com.github.tlbueno.artemis.scenarios.model.request.endpoint.consumer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.tlbueno.artemis.scenarios.model.request.endpoint.BaseEndpointMessage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.log4j.Log4j2;

@EqualsAndHashCode(callSuper = true)
@Getter
@Jacksonized
@Log4j2
@SuperBuilder(setterPrefix = "with")
@ToString(callSuper = true)
public class ConsumerEndpointMessage extends BaseEndpointMessage {
    @NonNull
    @Accessors(fluent = true)
    @JsonProperty(value = "sendReply")
    private Boolean isSendReply;
    private String msgSelector;
    @NonNull
    private Integer msgTimeoutInMs;
}
