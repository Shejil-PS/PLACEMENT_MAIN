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

public class GetDeclarationByIdHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetDeclarationByIdHandler.class);
    private final DeclarationService declarationService;

    public GetDeclarationByIdHandler(DeclarationService declarationService) {
        this.declarationService = declarationService;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();

        try {
            String id = routingContext.pathParam("id");

            Document doc = declarationService.getDeclarationById(id);
            if (doc != null) {
                ResponseUtil.processResponseDocumentWithoutZone(doc);
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.TWOHUNDRED,
                        new JsonObject(doc.toJson()),
                        new JsonArray().add("Declaration details fetched successfully"));
            } else {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.FILE_NOT_FOUND,
                        new JsonObject(),
                        new JsonArray().add("Declaration not found"));
            }

        } catch (Exception e) {
            LOGGER.error("Error in Get Declaration By Id Handler", e);
            ResponseUtil.createResponse(
                    response,
                    ResponseType.ERROR,
                    StatusCode.INTERNAL_SERVER_ERROR,
                    new JsonObject().put("error", "Internal Server Error"),
                    new JsonArray());
        }
    }
}
