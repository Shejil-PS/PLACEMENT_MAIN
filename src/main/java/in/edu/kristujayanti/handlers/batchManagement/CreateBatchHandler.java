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

import java.util.List;

import in.edu.kristujayanti.propertyBinder.placements.BatchesKeyPBinder;
import in.edu.kristujayanti.util.DocumentParser;

public class CreateBatchHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateBatchHandler.class);
    private final BatchService batchService;

    private final List<String> REQUIRED_FIELDS = List.of(
            BatchesKeyPBinder.BATCH_CODE.getPropertyName(),
            BatchesKeyPBinder.BATCH_NAME.getPropertyName(),
            BatchesKeyPBinder.DEPARTMENT_NAME.getPropertyName()
    );

    public CreateBatchHandler(BatchService batchService) {
        this.batchService = batchService;
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

            Document paramsDoc = Document.parse(body.encode());
            JsonArray validationResponse = DocumentParser.validateAndCleanDocument(paramsDoc, REQUIRED_FIELDS);

            if (!validationResponse.isEmpty()) {
                ResponseUtil.createResponse(response, ResponseType.VALIDATION, StatusCode.BAD_REQUEST, validationResponse, new JsonArray());
                return;
            }

            JsonObject createResult = batchService.createBatch(paramsDoc);
            if (!createResult.containsKey("error")) {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.CREATED,
                        createResult,
                        new JsonArray().add("Batch Creation Successful"));
            } else {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.BAD_REQUEST,
                        createResult,
                        new JsonArray().add("No Batch Created"));
            }
        } catch (Exception e) {
            LOGGER.error("Error in Create Batch Handler", e);
            ResponseUtil.createResponse(
                    response,
                    ResponseType.ERROR,
                    StatusCode.INTERNAL_SERVER_ERROR,
                    new JsonObject().put("error", "Internal Server Error"),
                    new JsonArray());
        }
    }
}
