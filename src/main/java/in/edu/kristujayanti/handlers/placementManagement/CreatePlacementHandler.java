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

public class CreatePlacementHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreatePlacementHandler.class);
    private final PlacementService placementService;

    private final List<String> REQUIRED_FIELDS = List.of(
            "companyId",
            "companyName",
            "batchCode",
            "driveStart",
            "driveEnd"
    );

    public CreatePlacementHandler(PlacementService placementService) {
        this.placementService = placementService;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();

        try {
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

            Document paramsDoc = Document.parse(body.encode());
            // If the user provided placementId alias, map it
            if (body.containsKey("placementId")) {
                paramsDoc.put("_id", body.getString("placementId"));
                paramsDoc.remove("placementId");
            }

            JsonObject createResult = placementService.createPlacement(paramsDoc);
            if (!createResult.containsKey("error")) {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.CREATED,
                        createResult,
                        new JsonArray().add("Placement Creation Successful"));
            } else {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.BAD_REQUEST,
                        createResult,
                        new JsonArray().add("No Placement Created"));
            }
        } catch (Exception e) {
            LOGGER.error("Error in Create Placement Handler", e);
            ResponseUtil.createResponse(
                    response,
                    ResponseType.ERROR,
                    StatusCode.INTERNAL_SERVER_ERROR,
                    new JsonObject().put("error", "Internal Server Error"),
                    new JsonArray());
        }
    }
}
