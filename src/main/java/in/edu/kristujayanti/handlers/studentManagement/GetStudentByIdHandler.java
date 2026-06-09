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

public class GetStudentByIdHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetStudentByIdHandler.class);
    private final StudentService studentService;

    public GetStudentByIdHandler(StudentService studentService) {
        this.studentService = studentService;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();

        try {
            String id = routingContext.pathParam("id");
            LOGGER.info("Handling request to get Student by ID: {}", id);

            Document student = studentService.getStudentById(id);
            if (student != null) {
                Object originalId = student.get("_id");
                if (originalId instanceof String) {
                    student.remove("_id");
                }
                ResponseUtil.processResponseDocumentWithoutZone(student);
                if (originalId instanceof String) {
                    student.put("_id", originalId);
                }
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.TWOHUNDRED,
                        JsonObject.mapFrom(student),
                        new JsonArray());
            } else {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.FILE_NOT_FOUND,
                        new JsonObject(),
                        new JsonArray().add("Student not found"));
            }
        } catch (Exception e) {
            LOGGER.error("Error in Get Student By ID Handler", e);
            ResponseUtil.createResponse(
                    response,
                    ResponseType.ERROR,
                    StatusCode.INTERNAL_SERVER_ERROR,
                    new JsonObject().put("error", "Internal Server Error"),
                    new JsonArray());
        }
    }
}
