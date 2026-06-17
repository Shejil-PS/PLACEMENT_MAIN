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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteDeclarationHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteDeclarationHandler.class);
    private final DeclarationService declarationService;

    public DeleteDeclarationHandler(DeclarationService declarationService) {
        this.declarationService = declarationService;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();

        try {
            String id = routingContext.pathParam("id");
            LOGGER.info("Handling request to delete Declaration by ID: {}", id);

            boolean deleted = declarationService.deleteDeclaration(id);
            if (deleted) {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.TWOHUNDRED,
                        new JsonObject().put("deleted", true),
                        new JsonArray().add("Declaration deleted successfully"));
            } else {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.FILE_NOT_FOUND,
                        new JsonObject(),
                        new JsonArray().add("Declaration not found"));
            }
        } catch (Exception e) {
            LOGGER.error("Error in Delete Declaration Handler", e);
            ResponseUtil.createResponse(
                    response,
                    ResponseType.ERROR,
                    StatusCode.INTERNAL_SERVER_ERROR,
                    new JsonObject().put("error", "Internal Server Error"),
                    new JsonArray());
        }
    }
}
