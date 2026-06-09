package in.edu.kristujayanti.handlers.applicationManagement;

import in.edu.kristujayanti.enums.ResponseType;
import in.edu.kristujayanti.enums.StatusCode;
import in.edu.kristujayanti.services.ApplicationService;
import in.edu.kristujayanti.util.ResponseUtil;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GetApplicationsHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetApplicationsHandler.class);
    private final ApplicationService applicationService;

    public GetApplicationsHandler(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();

        try {
            JsonObject filters = new JsonObject();
            extractParam(routingContext, filters, "placementId");
            extractParam(routingContext, filters, "jobId");
            extractParam(routingContext, filters, "companyId");
            extractParam(routingContext, filters, "studentId");
            extractParam(routingContext, filters, "status");

            LOGGER.info("Handling request to list applications with filters: {}", filters.encode());

            List<Document> applicationsList = applicationService.getAllApplications(filters);
            if (!applicationsList.isEmpty()) {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.TWOHUNDRED,
                        new JsonArray(applicationsList),
                        new JsonArray());
            } else {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.BAD_REQUEST,
                        new JsonArray(),
                        new JsonArray().add("No Applications Found"));
            }
        } catch (Exception e) {
            LOGGER.error("Error in Get Applications Handler", e);
            ResponseUtil.createResponse(
                    response,
                    ResponseType.ERROR,
                    StatusCode.INTERNAL_SERVER_ERROR,
                    new JsonObject().put("error", "Internal Server Error"),
                    new JsonArray());
        }
    }

    private void extractParam(RoutingContext ctx, JsonObject filters, String key) {
        String val = ctx.queryParam(key).stream().findFirst().orElse(null);
        if (val != null && !val.isBlank()) {
            filters.put(key, val);
        }
    }
}
