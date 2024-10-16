package com.github.tlbueno.artemis.scenarios.model.request.endpoint.producer;

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

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Getter
@Jacksonized
@Log4j2
@SuperBuilder(setterPrefix = "with")
@ToString(callSuper = true)
public class ProducerEndpointMessage extends BaseEndpointMessage {
    @NonNull
    private Integer deliveryMode;
    @NonNull
    @Accessors(fluent = true)
    @JsonProperty(value = "setReplyDestination")
    private Boolean isSetReplyDestination;
    private Map<String, String> msgProperties;
    @NonNull
    private Integer msgSizeInKb;
}
