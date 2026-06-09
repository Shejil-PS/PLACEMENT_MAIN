package in.edu.kristujayanti.handlers.companyManagement;

import in.edu.kristujayanti.enums.ResponseType;
import in.edu.kristujayanti.enums.StatusCode;
import in.edu.kristujayanti.handlers.HealthHandler;
import in.edu.kristujayanti.services.CompanyService;
import in.edu.kristujayanti.services.HealthService;
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

public class ListCompaniesHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListCompaniesHandler.class);
    private final CompanyService companyService;

    // Constructor
    public ListCompaniesHandler(CompanyService companyService) {
        this.companyService = companyService;
    }

    /**
     * Handles the request to fetch application status.
     *
     * @param routingContext the routing context
     */
    @Override
    public void handle(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();

        try {
            LOGGER.info("Handling request for Listing Companies");
            List<Document> companiesList = companyService.getAllCompanies();
            if(!companiesList.isEmpty()){
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.TWOHUNDRED,
                        new JsonArray(companiesList),
                        new JsonArray());
            }else{
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.BAD_REQUEST,
                        new JsonArray(),
                        new JsonArray().add("No Companies Created"));
            }


        } catch (Exception e) {
            LOGGER.error("Error in List Company Handler", e);
            ResponseUtil.createResponse(
                    response,
                    ResponseType.ERROR,
                    StatusCode.INTERNAL_SERVER_ERROR,
                    new JsonObject().put("error", "Internal Server Error"),
                    new JsonArray());
        }
    }
}