package org.apache.activemq.artemis.scenarios.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.apache.activemq.artemis.scenarios.model.requests.BusinessProcessRequest;
import org.apache.activemq.artemis.scenarios.model.response.BaseResponse;
import org.apache.activemq.artemis.scenarios.service.BusinessService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static org.apache.activemq.artemis.scenarios.resource.Constants.APPLICATION_YAML;

@Path("/businessProcess")
public class ProcessResource {

    private static final Logger LOGGER = LogManager.getLogger(ProcessResource.class);

    @POST
    @Consumes(APPLICATION_YAML)
    @Produces(APPLICATION_YAML)
    public Response startScenario(BusinessProcessRequest businessProcessRequest) throws Exception {
        BusinessService service = new BusinessService();
        BaseResponse result = service.process(businessProcessRequest);

        return Response.ok(result).build();
    }
}
