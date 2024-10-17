package org.apache.activemq.artemis.scenarios.model.request.endpoint;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.log4j.Log4j2;

@Builder(setterPrefix = "with")
@EqualsAndHashCode()
@Getter
@Jacksonized
@Log4j2
@ToString(callSuper = true)
public class EndpointConnection {
    @NonNull
    private Integer numOfConnections;
    @NonNull
    private Integer numOfSessionsPerConnection;
    private String password;
    @NonNull
    private String url;
    private String username;
}
