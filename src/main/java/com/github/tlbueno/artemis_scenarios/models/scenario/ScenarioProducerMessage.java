package com.github.tlbueno.artemis_scenarios.models.scenario;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Getter
@Log4j2
@Setter
@ToString(callSuper = true)
public class ScenarioProducerMessage extends ScenarioMessage {
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
