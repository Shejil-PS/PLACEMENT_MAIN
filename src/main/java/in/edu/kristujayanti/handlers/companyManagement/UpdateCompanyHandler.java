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

public class UpdateCompanyHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateCompanyHandler.class);
    private final CompanyService companyService;

    public UpdateCompanyHandler(CompanyService companyService) {
        this.companyService = companyService;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();

        try {
            String id = routingContext.pathParam("id");
            LOGGER.info("Handling request to update Company by ID: {}", id);

            JsonObject body = routingContext.body().asJsonObject();
            if (body == null || body.isEmpty()) {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.BAD_REQUEST,
                        new JsonObject(),
                        new JsonArray().add("Request body is required and cannot be empty"));
                return;
            }

            // Remove internal/immutable fields if passed in request body
            body.remove("_id");

            Document updateDoc = Document.parse(body.encode());
            Document updatedCompany = companyService.updateCompany(id, updateDoc);

            if (updatedCompany != null) {
                Object originalId = updatedCompany.get("_id");
                if (originalId instanceof String) {
                    updatedCompany.remove("_id");
                }
                ResponseUtil.processResponseDocumentWithoutZone(updatedCompany);
                if (originalId instanceof String) {
                    updatedCompany.put("_id", originalId);
                }

                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.TWOHUNDRED,
                        new JsonObject(updatedCompany),
                        new JsonArray().add("Company updated successfully"));
            } else {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.FILE_NOT_FOUND,
                        new JsonObject(),
                        new JsonArray().add("Company not found"));
            }
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid input or empty fields", e);
            ResponseUtil.createResponse(
                    response,
                    ResponseType.SUCCESS,
                    StatusCode.BAD_REQUEST,
                    new JsonObject(),
                    new JsonArray().add(e.getMessage()));
        } catch (Exception e) {
            LOGGER.error("Error in Update Company Handler", e);
            ResponseUtil.createResponse(
                    response,
                    ResponseType.ERROR,
                    StatusCode.INTERNAL_SERVER_ERROR,
                    new JsonObject().put("error", "Internal Server Error"),
                    new JsonArray());
        }
    }
}
