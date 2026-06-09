package in.edu.kristujayanti.services;

import com.mongodb.client.MongoDatabase;
import in.edu.kristujayanti.dbaccess.MongoDataAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HealthService extends MongoDataAccess {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthService.class);
    @SuppressWarnings("unused")
    private final MongoDatabase mongoDatabase;

    public HealthService(MongoDatabase mongoDatabase) {
        this.mongoDatabase = mongoDatabase;
    }

    /**
     * Returns a sample welcome message.
     *
     * @return Welcome message string.
     */
    public String getAppStatus() {
        LOGGER.info("Fetching application status message.");
        return "Microservice is up and running!";
    }
}
