package in.edu.kristujayanti;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import in.edu.kristujayanti.constants.FileUploadKeys;
import in.edu.kristujayanti.exception.BootstrapException;
import in.edu.kristujayanti.router.MicroserviceRouter;
import in.edu.kristujayanti.router.RouterBuilder;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisOptions;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class MicroserviceStarter {
    // Logger instance for logging information
    private static final Logger LOGGER = LoggerFactory.getLogger(MicroserviceStarter.class);

    // Vert.x instance, HTTP server, and router initialization
    private final Vertx vertx = Vertx.vertx();
    private final HttpServer server = vertx.createHttpServer();
    private final Router router = Router.router(vertx);
    private Redis redisCommandConnection; // Redis connection

    // List of valid build environments
    private static final List<String> BUILD_ENVIRONMENTS = Arrays.asList("development", "CI", "demo", "production");

    // Configuration keys
    private static final String REDIS_CONNECT_INFO = "redis-connect-info";
    private static final String MONGO_CONNECT_INFO = "mongo-connect-info";
    private static final String MICROSERVICE_APP_INFO = "placements-app-app-info";
    private static final String DB_NAME = "db_name";
    private static final String CONNECT_URL = "connectURL";

    // Main method to start the application
    public static void main(String[] args) {
        try {
            // Parse command-line arguments
            CommandLine commandParser = new DefaultParser().parse(getOptions(), args);
            String configServerURL = commandParser.getOptionValue("config_server_address");
            String environment = commandParser.getOptionValue("environment");

            LOGGER.info("Using configuration server(redis) cluster {} for bootstrapping on {} environment.",
                    configServerURL, environment);

            // Create MicroserviceStarter instance and start bootstrapping
            MicroserviceStarter microserviceStarter = new MicroserviceStarter();
            microserviceStarter.bootstrapApplication(configServerURL, environment);
        } catch (ParseException e) {
            LOGGER.error(
                    "Invalid command line arguments. Expected format: -config_server_address IP:PORT -environment buildMode.",
                    e);
            System.exit(0);
        } catch (BootstrapException e) {
            LOGGER.error(e.getMessage());
            System.exit(0);
        }
    }

    // Method to define command-line options
    private static Options getOptions() {
        Options options = new Options();
        options.addOption("config_server_address", true, "Configuration server IP & port");
        options.addOption("environment", true, "Environment development|CI|demo|production");
        return options;
    }

    // Method to bootstrap the application with given configuration server URL and
    // environment
    private void bootstrapApplication(String configServerURL, String environment) {
        validateConfigServerURL(configServerURL); // Validate the config server URL format
        validateEnvironment(environment); // Validate the environment

        // Key for application configuration based on the environment
        String appConfigKey = "app-config-values-" + environment;
        // Create a ConfigRetriever to fetch the configuration from the Redis server
        ConfigRetriever configRetriever = createConfigRetriever(configServerURL, appConfigKey);

        // Retrieve the configuration and handle the result
        configRetriever.getConfig(this::handleConfigRetrieval);
    }

    // Method to validate the configuration server URL
    private void validateConfigServerURL(String configServerURL) {
        if (configServerURL.split(":").length != 2) {
            throw new BootstrapException("Invalid format of configuration server URL. Expected format: IP:Port.");
        }
    }

    // Method to validate the environment value
    private void validateEnvironment(String environment) {
        if (!BUILD_ENVIRONMENTS.contains(environment)) {
            throw new BootstrapException(
                    "Invalid environment value. Expected values are: " + String.join("|", BUILD_ENVIRONMENTS));
        }
    }

    // Method to create a ConfigRetriever for fetching configuration from the Redis
    // server
    private ConfigRetriever createConfigRetriever(String configServerURL, String appConfigKey) {
        String[] tokens = configServerURL.split(":");
        ConfigStoreOptions storeOptions = new ConfigStoreOptions().setType("redis")
                .setConfig(
                        new JsonObject().put("endpoints", new JsonArray().add("redis://" + tokens[0] + ":" + tokens[1]))
                                .put("key", appConfigKey));
        return ConfigRetriever.create(vertx, new ConfigRetrieverOptions().addStore(storeOptions));
    }

    // Method to handle the configuration retrieval result
    private void handleConfigRetrieval(AsyncResult<JsonObject> asyncResult) {
        if (asyncResult.failed()) {
            LOGGER.error("Failed to retrieve config from server.", asyncResult.cause());
            System.exit(0);
        }

        JsonObject config = asyncResult.result();
        if (config == null) {
            LOGGER.error("Configuration from config server is null.");
            System.exit(0);
        }

        // Move blocking operations (file existence checks) to a worker thread using
        // Callable
        vertx.executeBlocking(() -> {
            // Setup database connections, routers, services, and HTTP server
            setupDatabaseConnections(config);
            // Check for required files in a blocking thread
            if (!config.containsKey(MICROSERVICE_APP_INFO)) {
                throw new BootstrapException(MICROSERVICE_APP_INFO + " missing in redis.");
            }
            JsonObject apiInfo = new JsonObject(config.getString(MICROSERVICE_APP_INFO));
            String rootDirectoryPath = apiInfo.getString(FileUploadKeys.ROOT_DIRECTORY_PATH);
            if (!new File(rootDirectoryPath).exists()) {
                throw new BootstrapException("Root Directory Path :-" + rootDirectoryPath + " does not exist");
            }
            String vertxFileBufferPathPath = apiInfo.getString(FileUploadKeys.VERTX_FILE_BUFFER_PATH);
            if (!new File(vertxFileBufferPathPath).exists()) {
                throw new BootstrapException("Vertx File Buffer Path :-" + vertxFileBufferPathPath + " does not exist");
            }

            // Non-blocking: setup routers/services/server
            setupRoutersAndServices(config);
            setupHttpServer(config);
            return config;
        }, res -> {
            if (res.failed()) {
                LOGGER.error("Blocking startup error: {}", res.cause().getMessage());
                System.exit(0);
            }

        });
    }

    // Method to set up database connections (Redis and MongoDB)
    private void setupDatabaseConnections(JsonObject config) {
        if (!config.containsKey(REDIS_CONNECT_INFO)) {
            LOGGER.error("Redis connect info missing from configuration server.");
            throw new BootstrapException("Redis connect info missing from configuration server.");
        }

        // Setup Redis connection
        JsonObject redisConfig = new JsonObject(config.getString(REDIS_CONNECT_INFO));
        RedisOptions redisOptions = createRedisOptions(redisConfig);
        redisCommandConnection = Redis.createClient(vertx, redisOptions);

        redisCommandConnection.connect(res -> {
            if (res.succeeded()) {
                LOGGER.info("Redis connection established.");
            } else {
                LOGGER.error("Failed to establish Redis connection.", res.cause());
                throw new BootstrapException("Failed to establish Redis connection.");
            }
        });

        // Check if MongoDB connection info is present
        if (!config.containsKey(MONGO_CONNECT_INFO)) {
            LOGGER.error("MongoDB connect info missing from configuration server.");
            throw new BootstrapException("MongoDB connect info missing from configuration server.");
        }
    }

    // Method to set up routers and services
    private void setupRoutersAndServices(JsonObject config) {
        JsonObject mongoConfig = new JsonObject(config.getString(MONGO_CONNECT_INFO));
        MongoClient mongoClient = getMongoClient(mongoConfig);
        MongoDatabase mongoDatabase = getMongoDatabase(mongoConfig, mongoClient);
        WebClient webClient = WebClient.create(vertx);

        // Check if ReportOrchestrator app info is present
        if (!config.containsKey(MICROSERVICE_APP_INFO)) {
            LOGGER.error("{} missing from configuration server.", MICROSERVICE_APP_INFO);
            System.exit(0);
        }

        JsonObject apiInfo = new JsonObject(config.getString(MICROSERVICE_APP_INFO));
        LOGGER.info("Setting up routers.");

        // File existence checks performed in executeBlocking

        try {
            // Initialize routers with the required configurations
            RouterBuilder routerBuilder = new RouterBuilder();
            routerBuilder.initialize(router, redisCommandConnection, mongoDatabase, mongoClient, apiInfo, webClient,
                    vertx);

            // Setup Microservice routers
            MicroserviceRouter microserviceRouter = new MicroserviceRouter(router, redisCommandConnection,
                    mongoDatabase, mongoClient, webClient, apiInfo, vertx);
            microserviceRouter.setUpRouters();
        } catch (Exception e) {
            LOGGER.error("Error initializing routers.", e);
            System.exit(0);
        }
    }

    // Method to set up the HTTP server
    private void setupHttpServer(JsonObject config) {
        JsonObject apiInfo = new JsonObject(config.getString(MICROSERVICE_APP_INFO));

        LOGGER.info("Setting up HTTP server.");
        server.requestHandler(router).listen(apiInfo.getInteger("port"), res -> {
            if (res.succeeded()) {
                LOGGER.info("Bootstrap complete. {} listening on port {}.", apiInfo.getString("api_name"),
                        apiInfo.getInteger("port"));
            } else {
                LOGGER.error("Failed to start HTTP server.", res.cause());
                System.exit(0);
            }
        });

        // Shutdown hook to clean up resources on application exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down Microservice application.");
            vertx.close();
            if (redisCommandConnection != null) {
                redisCommandConnection.close();
            }
            server.close();
        }));
    }

    // Method to create Redis options from the configuration
    private RedisOptions createRedisOptions(JsonObject redisConfig) {
        if (!redisConfig.containsKey(CONNECT_URL) || redisConfig.getJsonArray(CONNECT_URL).isEmpty()) {
            LOGGER.error("Redis connect URL missing from configuration server.");
            throw new BootstrapException("Redis connect URL missing from configuration server.");
        }

        RedisOptions redisOptions = new RedisOptions();
        redisConfig.getJsonArray(CONNECT_URL).forEach(url -> redisOptions.addConnectionString(url.toString()));
        return redisOptions;
    }

    // Method to get MongoDB client from the configuration
    private MongoClient getMongoClient(JsonObject mongoConfig) {
        if (!mongoConfig.containsKey("connection_string") || !mongoConfig.containsKey(DB_NAME)) {
            LOGGER.error("MongoDB connection string or DB name missing from configuration server.");
            throw new BootstrapException("MongoDB connection string or DB name missing from configuration server.");
        }

        String connectionString = mongoConfig.getString("connection_string");
        com.mongodb.ConnectionString connString = new com.mongodb.ConnectionString(connectionString);

        if (connString.getDatabase() == null) {
            String dbName = mongoConfig.getString(DB_NAME);
            if (connectionString.contains("?")) {
                int queryIdx = connectionString.indexOf("?");
                String base = connectionString.substring(0, queryIdx);
                String query = connectionString.substring(queryIdx);
                if (base.endsWith("/")) {
                    connectionString = base + dbName + query;
                } else {
                    connectionString = base + "/" + dbName + query;
                }
            } else {
                if (connectionString.endsWith("/")) {
                    connectionString = connectionString + dbName;
                } else {
                    connectionString = connectionString + "/" + dbName;
                }
            }
        }

        return MongoClients.create(connectionString);
    }

    // Method to get MongoDB database instance from the configuration
    private MongoDatabase getMongoDatabase(JsonObject mongoConfig, MongoClient mongoClient) {
        return mongoClient.getDatabase(mongoConfig.getString(DB_NAME));
    }
}