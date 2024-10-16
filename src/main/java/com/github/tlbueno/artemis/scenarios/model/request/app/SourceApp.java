package com.github.tlbueno.artemis.scenarios.model.request.app;

import com.github.tlbueno.artemis.scenarios.model.request.endpoint.producer.ProducerEndpoint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.log4j.Log4j2;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Getter
@Jacksonized
@Log4j2
@SuperBuilder(setterPrefix = "with")
@ToString(callSuper = true)
public class SourceApp extends AppBase {
    @NonNull
    private List<ProducerEndpoint> producers;
}
