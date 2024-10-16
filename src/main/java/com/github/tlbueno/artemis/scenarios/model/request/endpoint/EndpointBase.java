package com.github.tlbueno.artemis.scenarios.model.request.endpoint;

import com.github.tlbueno.artemis.scenarios.model.request.app.AppBase;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;

@EqualsAndHashCode(callSuper = true)
@Getter
@Log4j2
@SuperBuilder(setterPrefix = "with")
@ToString(callSuper = true)
public abstract class EndpointBase extends AppBase {
    @NonNull
    private EndpointConnection connection;
    @NonNull
    private EndpointSession session;
}
