package com.github.tlbueno.artemis.scenarios.model.request.app;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;

@EqualsAndHashCode()
@Getter
@Log4j2
@SuperBuilder(setterPrefix = "with")
@ToString(callSuper = true)
public abstract class AppBase {
    @NonNull
    private String id;

    @NonNull
    @Accessors(fluent = true)
    @JsonProperty(value = "enabled")
    private Boolean isEnabled;
}
