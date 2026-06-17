package in.edu.kristujayanti.handlers.declarationManagement;

import in.edu.kristujayanti.enums.ResponseType;
import in.edu.kristujayanti.enums.StatusCode;
import in.edu.kristujayanti.services.DeclarationService;
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

public class ListDeclarationsHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListDeclarationsHandler.class);
    private final DeclarationService declarationService;

    public ListDeclarationsHandler(DeclarationService declarationService) {
        this.declarationService = declarationService;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        HttpServerRequest request = routingContext.request();

        try {
            String search = request.getParam("search");
            List<Document> docList = declarationService.getAllDeclarations(search);

            JsonArray resArray = new JsonArray();
            for (Document d : docList) {
                resArray.add(new JsonObject(d.toJson()));
            }

            ResponseUtil.createResponse(
                    response,
                    ResponseType.SUCCESS,
                    StatusCode.TWOHUNDRED,
                    resArray,
                    new JsonArray().add("Declarations fetched successfully"));

        } catch (Exception e) {
            LOGGER.error("Error in List Declarations Handler", e);
            ResponseUtil.createResponse(
                    response,
                    ResponseType.ERROR,
                    StatusCode.INTERNAL_SERVER_ERROR,
                    new JsonObject().put("error", "Internal Server Error"),
                    new JsonArray());
        }
    }
}
