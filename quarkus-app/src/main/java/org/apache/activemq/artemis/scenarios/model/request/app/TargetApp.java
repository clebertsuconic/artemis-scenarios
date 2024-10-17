package org.apache.activemq.artemis.scenarios.model.request.app;

import org.apache.activemq.artemis.scenarios.model.request.endpoint.consumer.ConsumerEndpoint;
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
public class TargetApp extends AppBase {
    @NonNull
    private List<ConsumerEndpoint> consumers;
}
