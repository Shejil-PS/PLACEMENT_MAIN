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

public class ApplyForJobHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplyForJobHandler.class);
    private final ApplicationService applicationService;

    private final List<String> REQUIRED_FIELDS = List.of(
            "studentId",
            "rollNo",
            "studentName",
            "placementId",
            "jobId",
            "companyId",
            "companyName",
            "appliedDate"
    );

    public ApplyForJobHandler(ApplicationService applicationService) {
        this.applicationService = applicationService;
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

            JsonObject createResult = applicationService.applyForJob(paramsDoc);
            if (!createResult.containsKey("error")) {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.CREATED,
                        createResult,
                        new JsonArray().add("Application submitted successfully"));
            } else {
                String errorMsg = createResult.getString("error");
                StatusCode code = errorMsg.contains("already applied") ? StatusCode.CONFLICT : StatusCode.BAD_REQUEST;
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        code,
                        createResult,
                        new JsonArray().add(errorMsg));
            }
        } catch (Exception e) {
            LOGGER.error("Error in Apply For Job Handler", e);
            ResponseUtil.createResponse(
                    response,
                    ResponseType.ERROR,
                    StatusCode.INTERNAL_SERVER_ERROR,
                    new JsonObject().put("error", "Internal Server Error"),
                    new JsonArray());
        }
    }
}
