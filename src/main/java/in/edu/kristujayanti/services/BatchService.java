package in.edu.kristujayanti.services;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import in.edu.kristujayanti.dbaccess.MongoDataAccess;
import in.edu.kristujayanti.util.ResponseUtil;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.Response;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

public class BatchService extends MongoDataAccess {

    private final MongoDatabase mongoDatabase;
    private final MongoClient mongoClient;
    private final Redis redisClient;
    private static final String COLLECTION = "batches";

    public BatchService(MongoDatabase mongoDatabase, MongoClient mongoClient, Redis redisClient) {
        this.mongoDatabase = mongoDatabase;
        this.mongoClient = mongoClient;
        this.redisClient = redisClient;
    }

    private Bson buildQuery(String id) {
        List<Bson> orList = new ArrayList<>();
        if (id != null && id.length() == 24 && id.matches("^[0-9a-fA-F]{24}$")) {
            orList.add(com.mongodb.client.model.Filters.eq("_id", new ObjectId(id)));
        }
        orList.add(com.mongodb.client.model.Filters.eq("_id", id));
        orList.add(com.mongodb.client.model.Filters.eq("_id ", id));
        return com.mongodb.client.model.Filters.or(orList);
    }

    public List<Document> getAllBatches() {
        List<Document> batchesList = findDocuments(mongoDatabase, COLLECTION).into(new ArrayList<>());
        for (Document doc : batchesList) {
            Object originalId = doc.get("_id");
            if (originalId instanceof String) {
                doc.remove("_id");
            }
            ResponseUtil.processResponseDocumentWithoutZone(doc);
            if (originalId instanceof String) {
                doc.put("_id", originalId);
            }
        }
        return batchesList;
    }

    public Document getBatchById(String id) {
        Bson filter = buildQuery(id);
        Document doc = findSingleDocument(mongoDatabase, COLLECTION, filter);
        if (doc != null) {
            Object originalId = doc.get("_id");
            if (originalId instanceof String) {
                doc.remove("_id");
            }
            ResponseUtil.processResponseDocumentWithoutZone(doc);
            if (originalId instanceof String) {
                doc.put("_id", originalId);
            }
        }
        return doc;
    }

    public JsonObject createBatch(Document paramsDoc) {
        JsonObject result = new JsonObject();
        try (ClientSession clientSession = getMongoDbSession(mongoClient)) {
            if (!paramsDoc.containsKey("_id ") || paramsDoc.getString("_id ") == null || paramsDoc.getString("_id ").trim().isEmpty()) {
                try {
                    RedisAPI redisAPI = RedisAPI.api(redisClient);
                    Response response = redisAPI.incr("placement:batchId:counter")
                            .toCompletionStage()
                            .toCompletableFuture()
                            .get(5, java.util.concurrent.TimeUnit.SECONDS);
                    long counter = response.toLong();
                    paramsDoc.put("_id ", String.format("B%03d", counter));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to generate batch ID from Redis: " + e.getMessage());
                }
            }

            startTransaction(clientSession);
            boolean saved = saveDocument(COLLECTION, paramsDoc, clientSession, mongoDatabase);
            if (saved) {
                commitTransaction(clientSession);
                Object originalId = paramsDoc.get("_id");
                if (originalId instanceof String) {
                    paramsDoc.remove("_id");
                }
                ResponseUtil.processResponseDocumentWithoutZone(paramsDoc);
                if (originalId instanceof String) {
                    paramsDoc.put("_id", originalId);
                }
                return new JsonObject(paramsDoc.toJson());
            } else {
                abortTransaction(clientSession);
                return result.put("error", "Unable to create batch");
            }
        } catch (Exception e) {
            return result.put("error", "Unable to create batch: " + e.getMessage());
        }
    }

    public Document updateBatch(String id, Document updateDoc) {
        if (updateDoc.isEmpty()) {
            throw new IllegalArgumentException("No fields provided for update");
        }

        Document updateObj = new Document("$set", updateDoc);
        Bson filter = buildQuery(id);

        try (ClientSession clientSession = getMongoDbSession(mongoClient)) {
            startTransaction(clientSession);
            com.mongodb.client.model.FindOneAndUpdateOptions options = new com.mongodb.client.model.FindOneAndUpdateOptions()
                    .returnDocument(com.mongodb.client.model.ReturnDocument.AFTER);

            Document updatedDoc = mongoDatabase.getCollection(COLLECTION).findOneAndUpdate(clientSession, filter, updateObj, options);
            if (updatedDoc != null) {
                commitTransaction(clientSession);
                Object originalId = updatedDoc.get("_id");
                if (originalId instanceof String) {
                    updatedDoc.remove("_id");
                }
                ResponseUtil.processResponseDocumentWithoutZone(updatedDoc);
                if (originalId instanceof String) {
                    updatedDoc.put("_id", originalId);
                }
                return updatedDoc;
            } else {
                abortTransaction(clientSession);
                return null;
            }
        }
    }

    public boolean deleteBatch(String id) {
        Bson filter = buildQuery(id);
        try (ClientSession clientSession = getMongoDbSession(mongoClient)) {
            startTransaction(clientSession);
            boolean deleted = deleteDocument(COLLECTION, filter, clientSession, mongoDatabase);
            if (deleted) {
                commitTransaction(clientSession);
                return true;
            } else {
                abortTransaction(clientSession);
                return false;
            }
        }
    }
}
