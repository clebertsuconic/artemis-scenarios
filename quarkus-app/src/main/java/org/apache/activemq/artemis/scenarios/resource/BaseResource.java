package org.apache.activemq.artemis.scenarios.resource;

import org.apache.activemq.artemis.scenarios.ResourceManager;
import org.apache.activemq.artemis.scenarios.model.request.BaseScenarioRequest;
import org.apache.activemq.artemis.scenarios.model.response.ScenarioResponse;
import org.apache.activemq.artemis.scenarios.service.Scenario;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import java.util.List;

@EqualsAndHashCode()
@Log4j2
@ToString(callSuper = true)
public class BaseResource {
    protected static final String APPLICATION_YAML = "application/yaml";
    
    @DELETE
    @Path("/{id}")
    @Produces(APPLICATION_YAML)
    public Response deleteScenario(@PathParam("id") String id) {
        LOGGER.info("[{}] - Processing scenario delete request", id);
        ScenarioResponse scenarioResponse = ScenarioResponse.builder()
                .withId(id)
                .build();
        Scenario<? extends BaseScenarioRequest> scenario = ResourceManager.getScenario(id);
        if (scenario != null) {
            LOGGER.debug("[{}] - {}", id, scenario.toString());
            scenario.setKeepRunning(false);
            scenarioResponse.setResult("removed");
        } else {
            scenarioResponse.setResult("do not exists");
        }
        return Response.ok(scenarioResponse).build();
    }

    @GET
    @Path("/{id}")
    @Consumes(APPLICATION_YAML)
    @Produces(APPLICATION_YAML)
    public Response retrieveScenario(@PathParam("id") String id) {
        LOGGER.info("[{}] - Processing scenario retrieval request", id);
        Scenario<? extends BaseScenarioRequest> scenario = ResourceManager.getScenario(id);
        ScenarioResponse scenarioResponse = ScenarioResponse.builder()
                .withId(id)
                .build();
        if (scenario != null) {
            LOGGER.debug("[{}] - {}", id, scenario.toString());
            scenarioResponse.setResult("exists");
        } else {
            scenarioResponse.setResult("do not exists");
        }
        return Response.ok(scenarioResponse).build();
    }

    @GET
    @Consumes(APPLICATION_YAML)
    @Produces(APPLICATION_YAML)
    public Response retrieveScenarios() {
        String id = ResourceManager.generateUniqueUUID().toString();
        LOGGER.info("[{}] - Processing all scenarios retrieval request", id);
        ScenarioResponse scenarioResponse = ScenarioResponse.builder()
                .withId(ResourceManager.generateUniqueUUID().toString())
                .build();
        List<Scenario<? extends BaseScenarioRequest>> scenario = ResourceManager.getScenarios();
        StringBuilder result = new StringBuilder();
        scenario.forEach(e -> result.append(e.toString()).append(System.lineSeparator()));
        scenarioResponse.setResult(result.toString());
        return Response.ok(scenarioResponse).build();
    }

    protected void processScenarioCreation(Scenario<? extends BaseScenarioRequest> scenario) {
        String id = scenario.getId();
        LOGGER.info("[{}] - Processing scenario", id);
        ResourceManager.addScenario(id, scenario);
        scenario.processApps();
        while (true) {
            if (!scenario.isKeepRunning()) {
                break;
            }
        }
        LOGGER.info("[{}] - Finishing scenario", id);
        ResourceManager.removeScenario(id);
    }
}
