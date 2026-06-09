package in.edu.kristujayanti.handlers.studentManagement;

import in.edu.kristujayanti.enums.ResponseType;
import in.edu.kristujayanti.enums.StatusCode;
import in.edu.kristujayanti.services.StudentService;
import in.edu.kristujayanti.util.DocumentParser;
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

public class CreateStudentHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateStudentHandler.class);
    private final StudentService studentService;

    private final List<String> REQUIRED_FIELDS = List.of(
            "rollNo",
            "firstName",
            "lastName",
            "gender",
            "dob",
            "section",
            "specialization",
            "departmentName",
            "personalEmail",
            "batchCode"
    );

    public CreateStudentHandler(StudentService studentService) {
        this.studentService = studentService;
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

            JsonObject createResult = studentService.createStudent(paramsDoc);
            if (!createResult.containsKey("error")) {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.CREATED,
                        new JsonObject(),
                        new JsonArray().add("Student Creation Successful"));
            } else {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.BAD_REQUEST,
                        createResult,
                        new JsonArray().add("No Student Created"));
            }
        } catch (Exception e) {
            LOGGER.error("Error in Create Student Handler", e);
            ResponseUtil.createResponse(
                    response,
                    ResponseType.ERROR,
                    StatusCode.INTERNAL_SERVER_ERROR,
                    new JsonObject().put("error", "Internal Server Error"),
                    new JsonArray());
        }
    }
}
