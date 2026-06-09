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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteCompanyHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteCompanyHandler.class);
    private final CompanyService companyService;

    public DeleteCompanyHandler(CompanyService companyService) {
        this.companyService = companyService;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();

        try {
            String id = routingContext.pathParam("id");
            LOGGER.info("Handling request to delete Company by ID: {}", id);

            boolean deleted = companyService.deleteCompany(id);
            if (deleted) {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.TWOHUNDRED,
                        new JsonObject().put("deleted", true),
                        new JsonArray().add("Company deleted successfully"));
            } else {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.FILE_NOT_FOUND,
                        new JsonObject(),
                        new JsonArray().add("Company not found"));
            }
        } catch (Exception e) {
            LOGGER.error("Error in Delete Company Handler", e);
            ResponseUtil.createResponse(
                    response,
                    ResponseType.ERROR,
                    StatusCode.INTERNAL_SERVER_ERROR,
                    new JsonObject().put("error", "Internal Server Error"),
                    new JsonArray());
        }
    }
}
