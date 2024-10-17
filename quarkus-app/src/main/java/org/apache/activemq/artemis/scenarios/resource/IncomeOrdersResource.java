package org.apache.activemq.artemis.scenarios.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.activemq.artemis.scenarios.ResourceManager;
import org.apache.activemq.artemis.scenarios.model.request.NonTemporaryDestinationScenarioRequest;
import org.apache.activemq.artemis.scenarios.model.requests.OrdersIncome;
import org.apache.activemq.artemis.scenarios.model.response.ScenarioResponse;
import org.apache.activemq.artemis.scenarios.service.Scenario;

@EqualsAndHashCode(callSuper = true)
@Log4j2
@Path("/incomeOrders")
@ToString(callSuper = true)
public class IncomeOrdersResource extends BaseResource {
    @POST
    @Consumes(APPLICATION_YAML)
    @Produces(APPLICATION_YAML)
    public Response startScenario(OrdersIncome scenarioRequest) {
        String id = ResourceManager.generateUniqueUUID().toString();
        LOGGER.info("[{}] - Processing scenario create request", id);
        LOGGER.debug("[{}] - {}", id, scenarioRequest.toString());

        // TODO
        return null;
    }
}
