package org.apache.activemq.artemis.scenarios.model.request;

import org.apache.activemq.artemis.scenarios.model.request.app.SourceApp;
import org.apache.activemq.artemis.scenarios.model.request.app.TargetApp;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;

import java.util.List;

@EqualsAndHashCode()
@Getter
@Log4j2
@SuperBuilder(setterPrefix = "with")
@ToString(callSuper = true)
public abstract class BaseScenarioRequest {
    @NonNull
    private String destinationClass;
    @NonNull
    private String replyDestinationClass;
    @NonNull
    private List<SourceApp> sources;
    @NonNull
    private List<TargetApp> targets;
}
