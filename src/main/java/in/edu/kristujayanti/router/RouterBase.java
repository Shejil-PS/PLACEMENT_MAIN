package in.edu.kristujayanti.router;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.redis.client.Redis;

// Abstract base class for defining common properties and initialization for router classes
public abstract class RouterBase {

    // Protected members accessible by subclasses
    protected final Router router;                   // Vert.x Router instance for handling routes
    protected final Redis redisCommandConnection;    // Redis connection for command operations
    protected final MongoDatabase mongoDatabase;     // MongoDB database instance
    protected final MongoClient mongoClient;         // MongoDB client instance
    protected final WebClient client;                // Vert.x WebClient for making HTTP requests
    protected final JsonObject apiInfo;
    protected final Vertx vertx;

    // Constructor to initialize the common properties for subclasses
    RouterBase(Router router, Redis redisCommandConnection, MongoDatabase mongoDatabase, MongoClient mongoClient, WebClient client, JsonObject apiInfo, Vertx vertx) {
        this.router = router;
        this.redisCommandConnection = redisCommandConnection;
        this.mongoDatabase = mongoDatabase;
        this.mongoClient = mongoClient;
        this.client = client;
        this.apiInfo = apiInfo;
        this.vertx = vertx;
    }
}
