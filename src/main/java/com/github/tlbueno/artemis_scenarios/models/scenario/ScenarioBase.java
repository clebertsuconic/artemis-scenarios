package com.github.tlbueno.artemis_scenarios.models.scenario;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;

@EqualsAndHashCode()
@Getter
@Log4j2
@Setter
@ToString(callSuper = true)
public class ScenarioBase {
    @NonNull
    private String id;
    @NonNull
    @Accessors(fluent = true)
    @JsonProperty(value = "enabled")
    private Boolean isEnabled;
}
