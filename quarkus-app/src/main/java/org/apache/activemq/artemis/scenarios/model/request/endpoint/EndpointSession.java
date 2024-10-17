package org.apache.activemq.artemis.scenarios.model.request.endpoint;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.log4j.Log4j2;

@Builder(setterPrefix = "with")
@EqualsAndHashCode()
@Jacksonized
@Log4j2
@ToString(callSuper = true)
@Getter
public class EndpointSession {
    @NonNull
    @Accessors(fluent = true)
    @JsonProperty(value = "transactedSession")
    private Boolean isTransactedSession;
    @NonNull
    private Integer sessionAckMode;

}
