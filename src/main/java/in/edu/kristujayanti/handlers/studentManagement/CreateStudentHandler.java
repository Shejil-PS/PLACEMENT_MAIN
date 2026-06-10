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

import in.edu.kristujayanti.propertyBinder.placements.StudentsKeyPBinder;

public class CreateStudentHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateStudentHandler.class);
    private final StudentService studentService;

    private final List<String> REQUIRED_FIELDS = List.of(
            StudentsKeyPBinder.ROLL_NO.getPropertyName(),
            StudentsKeyPBinder.FIRST_NAME.getPropertyName(),
            StudentsKeyPBinder.LAST_NAME.getPropertyName(),
            StudentsKeyPBinder.GENDER.getPropertyName(),
            StudentsKeyPBinder.DOB.getPropertyName(),
            StudentsKeyPBinder.SECTION.getPropertyName(),
            StudentsKeyPBinder.SPECIALIZATION.getPropertyName(),
            StudentsKeyPBinder.DEPARTMENT_NAME.getPropertyName(),
            StudentsKeyPBinder.EMAIL.getPropertyName(),
            StudentsKeyPBinder.BATCH_CODE.getPropertyName()
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
            JsonArray validationResponse = DocumentParser.validateAndCleanDocument(paramsDoc, REQUIRED_FIELDS);

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
