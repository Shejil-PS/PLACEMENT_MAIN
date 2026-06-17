package in.edu.kristujayanti.handlers.companyManagement;

import in.edu.kristujayanti.enums.ResponseType;
import in.edu.kristujayanti.enums.StatusCode;
import in.edu.kristujayanti.services.CompanyService;
import in.edu.kristujayanti.util.ResponseUtil;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetCompanyByIdHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetCompanyByIdHandler.class);
    private final CompanyService companyService;

    public GetCompanyByIdHandler(CompanyService companyService) {
        this.companyService = companyService;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();

        try {
            String id = routingContext.pathParam("id");
            LOGGER.info("Handling request to get Company by ID: {}", id);

            Document company = companyService.getCompanyById(id);
            if (company != null) {
                Object originalId = company.get("_id");
                if (originalId instanceof String) {
                    company.remove("_id");
                }
                ResponseUtil.processResponseDocumentWithoutZone(company);
                if (originalId instanceof String) {
                    company.put("_id", originalId);
                }
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.TWOHUNDRED,
                        new JsonObject(company),
                        new JsonArray());
            } else {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.FILE_NOT_FOUND,
                        new JsonObject(),
                        new JsonArray().add("Company not found"));
            }
        } catch (Exception e) {
            LOGGER.error("Error in Get Company By ID Handler", e);
            ResponseUtil.createResponse(
                    response,
                    ResponseType.ERROR,
                    StatusCode.INTERNAL_SERVER_ERROR,
                    new JsonObject().put("error", "Internal Server Error"),
                    new JsonArray());
        }
    }
}
