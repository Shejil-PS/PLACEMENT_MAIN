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

public class GetPlacementsHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetPlacementsHandler.class);
    private final PlacementService placementService;

    public GetPlacementsHandler(PlacementService placementService) {
        this.placementService = placementService;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();

        try {
            String search = routingContext.request().getParam("search");
            List<Document> placementsList = placementService.getAllPlacements(search);
            JsonArray dataArray = new JsonArray();
            for (Document doc : placementsList) {
                dataArray.add(new JsonObject(doc.toJson()));
            }

            ResponseUtil.createResponse(
                    response,
                    ResponseType.SUCCESS,
                    StatusCode.TWOHUNDRED,
                    new JsonObject().put("data", dataArray),
                    new JsonArray().add("Successfully fetched placements"));

        } catch (Exception e) {
            LOGGER.error("Error in Get Placements Handler", e);
            ResponseUtil.createResponse(
                    response,
                    ResponseType.ERROR,
                    StatusCode.INTERNAL_SERVER_ERROR,
                    new JsonObject().put("error", "Internal Server Error"),
                    new JsonArray());
        }
    }
}
