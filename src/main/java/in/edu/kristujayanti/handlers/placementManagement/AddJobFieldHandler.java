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

import java.util.List;
import in.edu.kristujayanti.propertyBinder.placements.PlacementsKeyPBinder;

public class AddJobFieldHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddJobFieldHandler.class);
    private final PlacementService placementService;

    private final List<String> REQUIRED_FIELDS = List.of(
            PlacementsKeyPBinder.LABEL.getPropertyName(),
            PlacementsKeyPBinder.FIELD_TYPE.getPropertyName()
    );

    public AddJobFieldHandler(PlacementService placementService) {
        this.placementService = placementService;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();

        try {
            String placementId = routingContext.pathParam("placementId");
            String jobId = routingContext.pathParam("jobId");

            if (placementId == null || placementId.trim().isEmpty() || jobId == null || jobId.trim().isEmpty()) {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.VALIDATION,
                        StatusCode.BAD_REQUEST,
                        new JsonArray().add("placementId and jobId are required"),
                        new JsonArray());
                return;
            }

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

            JsonArray validationResponse = new JsonArray();
            for (String field : REQUIRED_FIELDS) {
                if (!body.containsKey(field) || body.getValue(field) == null || body.getValue(field).toString().trim().isEmpty()) {
                    validationResponse.add(field + " is required");
                }
            }

            if (!validationResponse.isEmpty()) {
                ResponseUtil.createResponse(response, ResponseType.VALIDATION, StatusCode.BAD_REQUEST, validationResponse, new JsonArray());
                return;
            }

            Document fieldDoc = Document.parse(body.encode());
            Document updatedPlacement = placementService.addJobField(placementId, jobId, fieldDoc);

            if (updatedPlacement != null) {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.CREATED,
                        new JsonObject().put("data", new JsonObject(updatedPlacement.toJson())),
                        new JsonArray().add("Job Field Added Successfully"));
            } else {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.FILE_NOT_FOUND,
                        new JsonObject(),
                        new JsonArray().add("Placement or job not found"));
            }
        } catch (Exception e) {
            LOGGER.error("Error in Add Job Field Handler", e);
            ResponseUtil.createResponse(
                    response,
                    ResponseType.ERROR,
                    StatusCode.INTERNAL_SERVER_ERROR,
                    new JsonObject().put("error", "Internal Server Error"),
                    new JsonArray());
        }
    }
}
