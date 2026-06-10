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

public class UpdatePlacementHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdatePlacementHandler.class);
    private final PlacementService placementService;

    public UpdatePlacementHandler(PlacementService placementService) {
        this.placementService = placementService;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();

        try {
            String placementId = routingContext.pathParam("placementId");
            if (placementId == null || placementId.trim().isEmpty()) {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.VALIDATION,
                        StatusCode.BAD_REQUEST,
                        new JsonArray().add("placementId is required"),
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
            Document updatedPlacement = placementService.updatePlacement(placementId, updateDoc);

            if (updatedPlacement != null) {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.TWOHUNDRED,
                        new JsonObject().put("data", new JsonObject(updatedPlacement.toJson())),
                        new JsonArray().add("Successfully updated placement"));
            } else {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.FILE_NOT_FOUND,
                        new JsonObject(),
                        new JsonArray().add("Placement not found or update failed"));
            }

        } catch (IllegalArgumentException e) {
            ResponseUtil.createResponse(
                    response,
                    ResponseType.VALIDATION,
                    StatusCode.BAD_REQUEST,
                    new JsonArray().add(e.getMessage()),
                    new JsonArray());
        } catch (Exception e) {
            LOGGER.error("Error in Update Placement Handler", e);
            ResponseUtil.createResponse(
                    response,
                    ResponseType.ERROR,
                    StatusCode.INTERNAL_SERVER_ERROR,
                    new JsonObject().put("error", "Internal Server Error"),
                    new JsonArray());
        }
    }
}
