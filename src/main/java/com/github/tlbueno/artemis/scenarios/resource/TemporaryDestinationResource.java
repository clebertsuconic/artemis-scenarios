package com.github.tlbueno.artemis.scenarios.resource;

import com.github.tlbueno.artemis.scenarios.ResourceManager;
import com.github.tlbueno.artemis.scenarios.model.request.TemporaryDestinationScenarioRequest;
import com.github.tlbueno.artemis.scenarios.model.response.ScenarioResponse;
import com.github.tlbueno.artemis.scenarios.service.Scenario;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

@EqualsAndHashCode(callSuper = true)
@Log4j2
@Path("/temporaryDestination")
@ToString(callSuper = true)
public class TemporaryDestinationResource extends BaseResource {
    @POST
    @Consumes(APPLICATION_YAML)
    @Produces(APPLICATION_YAML)
    public Response createScenario(TemporaryDestinationScenarioRequest scenarioRequest) {
        String id = ResourceManager.generateUniqueUUID().toString();
        LOGGER.info("[{}] - Processing scenario create request", id);
        LOGGER.debug("[{}] - {}", id, scenarioRequest.toString());
        ScenarioResponse scenarioResponse = ScenarioResponse.builder()
                .withId(id)
                .build();
        Scenario<TemporaryDestinationScenarioRequest> scenario = Scenario.<TemporaryDestinationScenarioRequest>builder()
                .id(id)
                .request(scenarioRequest)
                .response(scenarioResponse)
                .build();
        ResourceManager.submitTask(() -> processScenarioCreation(scenario));
        scenarioResponse.setResult("submitted");
        return Response.ok(scenarioResponse).build();
    }
}
