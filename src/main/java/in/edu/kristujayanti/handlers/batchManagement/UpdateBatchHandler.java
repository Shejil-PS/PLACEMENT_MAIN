package in.edu.kristujayanti.handlers.batchManagement;

import in.edu.kristujayanti.enums.ResponseType;
import in.edu.kristujayanti.enums.StatusCode;
import in.edu.kristujayanti.services.BatchService;
import in.edu.kristujayanti.util.ResponseUtil;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateBatchHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateBatchHandler.class);
    private final BatchService batchService;

    public UpdateBatchHandler(BatchService batchService) {
        this.batchService = batchService;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();

        try {
            String id = routingContext.pathParam("id");
            LOGGER.info("Handling request to update Batch by ID: {}", id);

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

            Document updateDoc = new Document();
            if (body.containsKey("batchCode")) {
                updateDoc.put("batchCode", body.getString("batchCode"));
            }
            if (body.containsKey("batchName")) {
                updateDoc.put("batchName", body.getString("batchName"));
            }
            if (body.containsKey("department")) {
                updateDoc.put("department", body.getString("department"));
            }

            if (updateDoc.isEmpty()) {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.BAD_REQUEST,
                        new JsonObject(),
                        new JsonArray().add("No fields provided for update"));
                return;
            }

            Document updatedBatch = batchService.updateBatch(id, updateDoc);
            if (updatedBatch != null) {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.TWOHUNDRED,
                        new JsonObject(updatedBatch.toJson()),
                        new JsonArray().add("Batch Update Successful"));
            } else {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.FILE_NOT_FOUND,
                        new JsonObject(),
                        new JsonArray().add("Batch not found"));
            }
        } catch (Exception e) {
            LOGGER.error("Error in Update Batch Handler", e);
            ResponseUtil.createResponse(
                    response,
                    ResponseType.ERROR,
                    StatusCode.INTERNAL_SERVER_ERROR,
                    new JsonObject().put("error", "Internal Server Error"),
                    new JsonArray());
        }
    }
}
