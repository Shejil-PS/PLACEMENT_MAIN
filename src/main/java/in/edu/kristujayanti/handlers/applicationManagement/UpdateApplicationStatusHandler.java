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

public class UpdateApplicationStatusHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateApplicationStatusHandler.class);
    private final ApplicationService applicationService;

    public UpdateApplicationStatusHandler(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();

        try {
            String applicationId = routingContext.pathParam("applicationId");
            LOGGER.info("Handling status update request for Application ID: {}", applicationId);

            JsonObject body = routingContext.body().asJsonObject();
            if (body == null) {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.BAD_REQUEST,
                        new JsonObject(),
                        new JsonArray().add("Request body is required"));
                return;
            }

            String status = body.getString("status");
            if (status == null || status.isBlank()) {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.BAD_REQUEST,
                        new JsonObject(),
                        new JsonArray().add("status field is required"));
                return;
            }

            Document updatedApp = applicationService.updateStatus(applicationId, status);
            if (updatedApp != null) {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.TWOHUNDRED,
                        new JsonObject(updatedApp.toJson()),
                        new JsonArray().add("Application status updated successfully"));
            } else {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.FILE_NOT_FOUND,
                        new JsonObject(),
                        new JsonArray().add("Application not found"));
            }
        } catch (IllegalArgumentException e) {
            ResponseUtil.createResponse(
                    response,
                    ResponseType.SUCCESS,
                    StatusCode.BAD_REQUEST,
                    new JsonObject(),
                    new JsonArray().add(e.getMessage()));
        } catch (Exception e) {
            LOGGER.error("Error in Update Application Status Handler", e);
            ResponseUtil.createResponse(
                    response,
                    ResponseType.ERROR,
                    StatusCode.INTERNAL_SERVER_ERROR,
                    new JsonObject().put("error", "Internal Server Error"),
                    new JsonArray());
        }
    }
}
