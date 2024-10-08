package com.github.tlbueno.artemis_scenarios.models.scenario;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Getter
@Log4j2
@Setter
@ToString(callSuper = true)
public class ScenarioRequest extends ScenarioBase {
    @NonNull
    private String destinationClass;
    private String destinationName;
    @NonNull
    private String replyDestinationClass;
    private String replyDestinationName;
    @NonNull
    private List<ScenarioSource> sources;
    @NonNull
    private List<ScenarioTarget> targets;
}
