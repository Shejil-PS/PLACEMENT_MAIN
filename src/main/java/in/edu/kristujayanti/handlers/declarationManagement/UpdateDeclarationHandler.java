package in.edu.kristujayanti.handlers.declarationManagement;

import in.edu.kristujayanti.enums.ResponseType;
import in.edu.kristujayanti.enums.StatusCode;
import in.edu.kristujayanti.services.DeclarationService;
import in.edu.kristujayanti.util.ResponseUtil;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateDeclarationHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateDeclarationHandler.class);
    private final DeclarationService declarationService;

    public UpdateDeclarationHandler(DeclarationService declarationService) {
        this.declarationService = declarationService;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();

        try {
            String id = routingContext.pathParam("id");
            JsonObject body = routingContext.body().asJsonObject();

            if (id == null || id.isEmpty()) {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.ERROR,
                        StatusCode.BAD_REQUEST,
                        new JsonObject().put("error", "Declaration ID is required"),
                        new JsonArray());
                return;
            }

            if (body == null || body.isEmpty()) {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.ERROR,
                        StatusCode.BAD_REQUEST,
                        new JsonObject().put("error", "Request body cannot be empty"),
                        new JsonArray());
                return;
            }

            Document updateDoc = Document.parse(body.encode());
            Document updatedObj = declarationService.updateDeclaration(id, updateDoc);

            if (updatedObj != null) {
                ResponseUtil.processResponseDocumentWithoutZone(updatedObj);
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.TWOHUNDRED,
                        new JsonObject(updatedObj.toJson()),
                        new JsonArray().add("Declaration updated successfully"));
            } else {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.BAD_REQUEST,
                        new JsonObject(),
                        new JsonArray().add("Unable to update Declaration"));
            }

        } catch (IllegalArgumentException e) {
            ResponseUtil.createResponse(
                    response,
                    ResponseType.ERROR,
                    StatusCode.BAD_REQUEST,
                    new JsonObject().put("error", e.getMessage()),
                    new JsonArray());
        } catch (Exception e) {
            LOGGER.error("Error in Update Declaration Handler", e);
            ResponseUtil.createResponse(
                    response,
                    ResponseType.ERROR,
                    StatusCode.INTERNAL_SERVER_ERROR,
                    new JsonObject().put("error", "Internal Server Error"),
                    new JsonArray());
        }
    }
}
