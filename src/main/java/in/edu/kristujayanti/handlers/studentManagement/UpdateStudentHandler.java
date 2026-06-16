package in.edu.kristujayanti.handlers.studentManagement;

import in.edu.kristujayanti.enums.ResponseType;
import in.edu.kristujayanti.enums.StatusCode;
import in.edu.kristujayanti.services.StudentService;
import in.edu.kristujayanti.util.ResponseUtil;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateStudentHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateStudentHandler.class);
    private final StudentService studentService;

    public UpdateStudentHandler(StudentService studentService) {
        this.studentService = studentService;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();

        try {
            String id = routingContext.pathParam("id");
            LOGGER.info("Handling request to update Student by ID: {}", id);

            JsonObject body = routingContext.body().asJsonObject();
            if (body == null || body.isEmpty()) {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.BAD_REQUEST,
                        new JsonObject(),
                        new JsonArray().add("Request body is r" +
                                "equired and cannot be empty"));
                return;
            }

            body.remove("_id");

            Document updateDoc = Document.parse(body.encode());
            Document updatedStudent = studentService.updateStudent(id, updateDoc);

            if (updatedStudent != null) {
                Object originalId = updatedStudent.get("_id");
                if (originalId instanceof String) {
                    updatedStudent.remove("_id");
                }
                ResponseUtil.processResponseDocumentWithoutZone(updatedStudent);
                if (originalId instanceof String) {
                    updatedStudent.put("_id", originalId);
                }

                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.TWOHUNDRED,
                        JsonObject.mapFrom(updatedStudent),
                        new JsonArray().add("Student updated successfully"));
            } else {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.FILE_NOT_FOUND,
                        new JsonObject(),
                        new JsonArray().add("Student not found"));
            }
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid input or empty fields", e);
            ResponseUtil.createResponse(
                    response,
                    ResponseType.SUCCESS,
                    StatusCode.BAD_REQUEST,
                    new JsonObject(),
                    new JsonArray().add(e.getMessage()));
        } catch (Exception e) {
            LOGGER.error("Error in Update Student Handler", e);
            ResponseUtil.createResponse(
                    response,
                    ResponseType.ERROR,
                    StatusCode.INTERNAL_SERVER_ERROR,
                    new JsonObject().put("error", "Internal Server Error"),
                    new JsonArray());
        }
    }
}
