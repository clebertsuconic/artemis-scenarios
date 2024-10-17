package org.apache.activemq.artemis.scenarios.model.request.endpoint.producer;

import org.apache.activemq.artemis.scenarios.model.request.endpoint.EndpointBase;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.log4j.Log4j2;

@EqualsAndHashCode(callSuper = true)
@Getter
@Jacksonized
@Log4j2
@SuperBuilder(setterPrefix = "with")
@ToString(callSuper = true)
public class ProducerEndpoint extends EndpointBase {
    @NonNull
    private ProducerEndpointMessage message;
}
