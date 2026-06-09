package in.edu.kristujayanti.constants;

/**
 * MicroserviceRoutingURLNames interface defines constants for various
 * routing URL paths used in the application.
 * These constants can be used throughout the application to ensure consistency
 * and avoid hardcoding URL paths.
 */
public interface MicroserviceRoutingURLNames {
    // Wildcard URL path for general API routing
    String API_WILDCARD_URL = "*";

    String HEALTH_URL = "/health";

    String LIST_COMPANIES= "/list-comnpanies";
    String CREATE_COMPANY = "/create-company";
    String GET_COMPANY_BY_ID = "/get-company/:id";
    String UPDATE_COMPANY = "/update-company/:id";
    String DELETE_COMPANY = "/delete-company/:id";

    String CREATE_STUDENT = "/create-student";
    String LIST_STUDENTS = "/list-students";
    String GET_STUDENT_BY_ID = "/get-student/:id";
    String UPDATE_STUDENT = "/update-student/:id";
    String DELETE_STUDENT = "/delete-student/:id";

    String CREATE_BATCH = "/create-batch";
    String LIST_BATCHES = "/list-batches";
    String GET_BATCH_BY_ID = "/get-batch/:id";
    String UPDATE_BATCH = "/update-batch/:id";
    String DELETE_BATCH = "/delete-batch/:id";

    String APPLY_FOR_JOB = "/applications";
    String LIST_APPLICATIONS = "/applications";
    String EXPORT_APPLICATIONS = "/applications/export";
    String GET_APPLICATION_BY_ID = "/applications/:applicationId";
    String UPDATE_APPLICATION_STATUS = "/applications/:applicationId/status";

}
