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

}
