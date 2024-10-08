package com.github.tlbueno.artemis_scenarios.models.scenario;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

@EqualsAndHashCode()
@Getter
@Log4j2
@Setter
@ToString(callSuper = true)
public class ScenarioConnection {
    @NonNull
    private Integer numOfConnections;
    @NonNull
    private Integer numOfSessionsPerConnection;
    private String password;
    @NonNull
    private String url;
    private String username;
}
