package in.edu.kristujayanti.handlers.placementManagement;

import in.edu.kristujayanti.enums.ResponseType;
import in.edu.kristujayanti.enums.StatusCode;
import in.edu.kristujayanti.services.PlacementService;
import in.edu.kristujayanti.util.ResponseUtil;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateJobFieldHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateJobFieldHandler.class);
    private final PlacementService placementService;

    public UpdateJobFieldHandler(PlacementService placementService) {
        this.placementService = placementService;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();

        try {
            String placementId = routingContext.pathParam("placementId");
            String jobId = routingContext.pathParam("jobId");
            String fieldId = routingContext.pathParam("fieldId");

            if (placementId == null || placementId.trim().isEmpty() ||
                jobId == null || jobId.trim().isEmpty() ||
                fieldId == null || fieldId.trim().isEmpty()) {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.VALIDATION,
                        StatusCode.BAD_REQUEST,
                        new JsonArray().add("placementId, jobId, and fieldId are required"),
                        new JsonArray());
                return;
            }

            JsonObject body = routingContext.body().asJsonObject();
            if (body == null || body.isEmpty()) {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.BAD_REQUEST,
                        new JsonObject(),
                        new JsonArray().add("Request body is required and cannot be empty"));
                return;
            }

            Document updateDoc = Document.parse(body.encode());
            Document updatedPlacement = placementService.updateJobField(placementId, jobId, fieldId, updateDoc);

            if (updatedPlacement != null) {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.TWOHUNDRED,
                        new JsonObject().put("data", new JsonObject(updatedPlacement.toJson())),
                        new JsonArray().add("Successfully updated job field"));
            } else {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.FILE_NOT_FOUND,
                        new JsonObject(),
                        new JsonArray().add("Placement, job, or field not found"));
            }

        } catch (Exception e) {
            LOGGER.error("Error in Update Job Field Handler", e);
            ResponseUtil.createResponse(
                    response,
                    ResponseType.ERROR,
                    StatusCode.INTERNAL_SERVER_ERROR,
                    new JsonObject().put("error", "Internal Server Error"),
                    new JsonArray());
        }
    }
}
