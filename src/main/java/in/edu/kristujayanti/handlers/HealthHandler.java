package in.edu.kristujayanti.handlers;

import in.edu.kristujayanti.enums.ResponseType;
import in.edu.kristujayanti.enums.StatusCode;
import in.edu.kristujayanti.services.HealthService;
import in.edu.kristujayanti.util.ResponseUtil;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HealthHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthHandler.class);
    private final HealthService healthService;

    // Constructor
    public HealthHandler(HealthService healthService) {
        this.healthService = healthService;
    }

    /**
     * Handles the request to fetch application status.
     *
     * @param routingContext the routing context
     */
    @Override
    public void handle(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();

        try {
            LOGGER.info("Handling request for HealthHandler");
            String res = healthService.getAppStatus();

            ResponseUtil.createResponse(
                    response,
                    ResponseType.SUCCESS,
                    StatusCode.TWOHUNDRED,
                    new JsonObject().put("message", res),
                    new JsonArray());
        } catch (Exception e) {
            LOGGER.error("Error in SampleHandler", e);
            ResponseUtil.createResponse(
                    response,
                    ResponseType.ERROR,
                    StatusCode.INTERNAL_SERVER_ERROR,
                    new JsonObject().put("error", "Internal Server Error"),
                    new JsonArray());
        }
    }
}