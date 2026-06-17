package in.edu.kristujayanti.handlers.declarationManagement;

import in.edu.kristujayanti.enums.ResponseType;
import in.edu.kristujayanti.enums.StatusCode;
import in.edu.kristujayanti.propertyBinder.placements.DeclarationKeyPBinder;
import in.edu.kristujayanti.services.DeclarationService;
import in.edu.kristujayanti.util.DocumentParser;
import in.edu.kristujayanti.util.ResponseUtil;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CreateDeclarationHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateDeclarationHandler.class);
    private final DeclarationService declarationService;

    private final List<String> REQUIRED_FIELDS = List.of(
            DeclarationKeyPBinder.DECLARATION_FORM.getPropertyName());

    public CreateDeclarationHandler(DeclarationService declarationService) {
        this.declarationService = declarationService;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();

        try {
            JsonObject body = routingContext.body().asJsonObject();
            Document paramsDoc = Document.parse(body.encode());

            JsonArray validationResponse = DocumentParser.validateAndCleanDocument(paramsDoc, REQUIRED_FIELDS);

            if (!validationResponse.isEmpty()) {
                ResponseUtil.createResponse(response, ResponseType.VALIDATION, StatusCode.BAD_REQUEST,
                        validationResponse, new JsonArray());
                return;
            }

            JsonObject createDeclaration = declarationService.createDeclaration(paramsDoc);
            if (!createDeclaration.containsKey("error")) {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.TWOHUNDRED,
                        new JsonArray(),
                        new JsonArray().add("Declaration Creation Successful"));
            } else {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.BAD_REQUEST,
                        createDeclaration,
                        new JsonArray().add("No Declarations Created"));
            }

        } catch (Exception e) {
            LOGGER.error("Error in Create Declaration Handler", e);
            ResponseUtil.createResponse(
                    response,
                    ResponseType.ERROR,
                    StatusCode.INTERNAL_SERVER_ERROR,
                    new JsonObject().put("error", "Internal Server Error"),
                    new JsonArray());
        }
    }
}
