package in.edu.kristujayanti.handlers.companyManagement;

import in.edu.kristujayanti.enums.ResponseType;
import in.edu.kristujayanti.enums.StatusCode;
import in.edu.kristujayanti.services.CompanyService;
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

public class CreateCompanyHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateCompanyHandler.class);
    private final CompanyService companyService;

    private final List<String> REQUIRED_FIELDS = List.of(
            "companyName",
            "industry",
            "contactPerson"
    );

    // Constructor
    public CreateCompanyHandler(CompanyService companyService) {
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
        HttpServerRequest request = routingContext.request();

        try {

            JsonObject body = routingContext.body().asJsonObject();

            Document paramsDoc = Document.parse(body.encode());

            JsonArray validationResponse = DocumentParser.validateAndCleanDocument(paramsDoc, REQUIRED_FIELDS);

            if(!validationResponse.isEmpty()){
                ResponseUtil.createResponse(response, ResponseType.VALIDATION, StatusCode.TWOHUNDRED, validationResponse, new JsonArray());
                return;
            }

           JsonObject createCompany = companyService.createCompany(paramsDoc);
            if(!createCompany.containsKey("error")){
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.TWOHUNDRED,
                        new JsonArray(),
                        new JsonArray().add("Company Creation Successful"));
            }else{
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.BAD_REQUEST,
                        createCompany,
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