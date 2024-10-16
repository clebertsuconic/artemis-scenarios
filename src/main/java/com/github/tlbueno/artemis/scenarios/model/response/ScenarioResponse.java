package com.github.tlbueno.artemis.scenarios.model.response;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.log4j.Log4j2;

@EqualsAndHashCode()
@Getter
@Log4j2
@ToString(callSuper = true)
public class ScenarioResponse {
    @NonNull
    private final String id;
    @Setter
    private String result;

    @Builder(setterPrefix = "with")
    @Jacksonized
    private ScenarioResponse(@NonNull String id) {
        this.id = id;
    }
}
