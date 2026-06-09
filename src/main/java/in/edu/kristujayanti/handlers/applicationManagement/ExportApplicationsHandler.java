package in.edu.kristujayanti.handlers.applicationManagement;

import in.edu.kristujayanti.enums.ResponseType;
import in.edu.kristujayanti.enums.StatusCode;
import in.edu.kristujayanti.services.ApplicationService;
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

public class ExportApplicationsHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExportApplicationsHandler.class);
    private final ApplicationService applicationService;

    public ExportApplicationsHandler(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();

        try {
            String placementId = routingContext.queryParam("placementId").stream().findFirst().orElse(null);
            if (placementId == null || placementId.isBlank()) {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.BAD_REQUEST,
                        new JsonObject(),
                        new JsonArray().add("placementId query param is required"));
                return;
            }

            String jobId = routingContext.queryParam("jobId").stream().findFirst().orElse(null);
            String status = routingContext.queryParam("status").stream().findFirst().orElse(null);
            String format = routingContext.queryParam("format").stream().findFirst().orElse("csv");

            LOGGER.info("Exporting applications for placementId: {}, jobId: {}, format: {}", placementId, jobId, format);

            List<Document> list = applicationService.exportApplications(placementId, jobId, status);
            JsonArray data = new JsonArray(list);

            if ("json".equalsIgnoreCase(format)) {
                ResponseUtil.createResponse(
                        response,
                        ResponseType.SUCCESS,
                        StatusCode.TWOHUNDRED,
                        data,
                        new JsonArray());
            } else {
                String csv = toCsv(data);
                response.setStatusCode(200)
                        .putHeader("Content-Type", "text/csv")
                        .putHeader("Content-Disposition", "attachment; filename=\"applications_" + placementId + ".csv\"")
                        .end(csv);
            }
        } catch (Exception e) {
            LOGGER.error("Error in Export Applications Handler", e);
            ResponseUtil.createResponse(
                    response,
                    ResponseType.ERROR,
                    StatusCode.INTERNAL_SERVER_ERROR,
                    new JsonObject().put("error", "Internal Server Error"),
                    new JsonArray());
        }
    }

    private String toCsv(JsonArray data) {
        StringBuilder sb = new StringBuilder();

        sb.append("applicationId,studentId,rollNo,studentName,placementId,jobId,")
          .append("companyId,companyName,appliedDate,status,resumeUrl\n");

        for (int i = 0; i < data.size(); i++) {
            JsonObject doc = data.getJsonObject(i);
            sb.append(csvValue(doc.getString("applicationId"))).append(',')
              .append(csvValue(doc.getString("studentId"))).append(',')
              .append(csvValue(doc.getString("rollNo"))).append(',')
              .append(csvValue(doc.getString("studentName"))).append(',')
              .append(csvValue(doc.getString("placementId"))).append(',')
              .append(csvValue(doc.getString("jobId"))).append(',')
              .append(csvValue(doc.getString("companyId"))).append(',')
              .append(csvValue(doc.getString("companyName"))).append(',')
              .append(csvValue(doc.getString("appliedDate"))).append(',')
              .append(csvValue(doc.getString("status"))).append(',')
              .append(csvValue(doc.getString("resumeUrl")))
              .append('\n');
        }

        return sb.toString();
    }

    private String csvValue(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
