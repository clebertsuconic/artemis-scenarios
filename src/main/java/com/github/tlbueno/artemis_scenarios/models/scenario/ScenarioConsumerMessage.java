package com.github.tlbueno.artemis_scenarios.models.scenario;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;

@EqualsAndHashCode(callSuper = true)
@Getter
@Log4j2
@Setter
@ToString(callSuper = true)
public class ScenarioConsumerMessage extends ScenarioMessage {
    @NonNull
    @Accessors(fluent = true)
    @JsonProperty(value = "sendReply")
    private Boolean isSendReply;
    private String msgSelector;
    @NonNull
    private Integer msgTimeoutInMs;
}
