package in.edu.kristujayanti.services;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
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
import java.util.UUID;

public class PlacementService extends MongoDataAccess {

    private final MongoDatabase mongoDatabase;
    private final MongoClient mongoClient;
    private final Redis redisClient;
    private static final String COLLECTION = "placements";

    public PlacementService(MongoDatabase mongoDatabase, MongoClient mongoClient, Redis redisClient) {
        this.mongoDatabase = mongoDatabase;
        this.mongoClient = mongoClient;
        this.redisClient = redisClient;
    }

    private Bson buildQuery(String id) {
        List<Bson> orList = new ArrayList<>();
        if (id != null && id.length() == 24 && id.matches("^[0-9a-fA-F]{24}$")) {
            orList.add(Filters.eq("_id", new ObjectId(id)));
        }
        orList.add(Filters.eq("_id", id));
        return Filters.or(orList);
    }

    // ── Placement CRUD ─────────────────────────────────────────────────────────

    public List<Document> getAllPlacements() {
        List<Document> list = findDocuments(mongoDatabase, COLLECTION).into(new ArrayList<>());
        for (Document doc : list) {
            Object originalId = doc.get("_id");
            if (originalId instanceof String) {
                doc.remove("_id");
            }
            ResponseUtil.processResponseDocumentWithoutZone(doc);
            if (originalId instanceof String) {
                doc.put("_id", originalId);
            }
        }
        return list;
    }

    public Document getPlacementById(String id) {
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

    public JsonObject createPlacement(Document paramsDoc) {
        JsonObject result = new JsonObject();
        try (ClientSession clientSession = getMongoDbSession(mongoClient)) {
            // Generate Placement ID if not present
            if (!paramsDoc.containsKey("_id") || paramsDoc.getString("_id") == null || paramsDoc.getString("_id").trim().isEmpty()) {
                try {
                    RedisAPI redisAPI = RedisAPI.api(redisClient);
                    Response response = redisAPI.incr("placement:placementId:counter")
                            .toCompletionStage()
                            .toCompletableFuture()
                            .get(5, java.util.concurrent.TimeUnit.SECONDS);
                    long counter = response.toLong();
                    String generatedId = String.format("P%03d", counter);
                    paramsDoc.put("_id", generatedId);
                    if (!paramsDoc.containsKey("placementCode_PlacementDrive_Text") || paramsDoc.getString("placementCode_PlacementDrive_Text") == null || paramsDoc.getString("placementCode_PlacementDrive_Text").trim().isEmpty()) {
                        paramsDoc.put("placementCode_PlacementDrive_Text", generatedId);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to generate placement ID from Redis: " + e.getMessage());
                }
            } else {
                if (!paramsDoc.containsKey("placementCode_PlacementDrive_Text") || paramsDoc.getString("placementCode_PlacementDrive_Text") == null || paramsDoc.getString("placementCode_PlacementDrive_Text").trim().isEmpty()) {
                    paramsDoc.put("placementCode_PlacementDrive_Text", paramsDoc.getString("_id"));
                }
            }

            // Generate IDs for any nested jobs/fields
            if (paramsDoc.containsKey("jobs_PlacementDrive_DocumentArray")) {
                List<Document> jobs = paramsDoc.getList("jobs_PlacementDrive_DocumentArray", Document.class);
                RedisAPI redisAPI = RedisAPI.api(redisClient);
                for (Document job : jobs) {
                    if (!job.containsKey("jobId_PlacementDrive_Text") || job.getString("jobId_PlacementDrive_Text") == null || job.getString("jobId_PlacementDrive_Text").trim().isEmpty()) {
                        try {
                            Response response = redisAPI.incr("job:jobId:counter")
                                    .toCompletionStage()
                                    .toCompletableFuture()
                                    .get(5, java.util.concurrent.TimeUnit.SECONDS);
                            job.put("jobId_PlacementDrive_Text", String.format("J%03d", response.toLong()));
                        } catch (Exception e) {
                            job.put("jobId_PlacementDrive_Text", "J-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                        }
                    }
                    if (job.containsKey("fields_PlacementDrive_DocumentArray")) {
                        List<Document> fields = job.getList("fields_PlacementDrive_DocumentArray", Document.class);
                        for (Document field : fields) {
                            if (!field.containsKey("fieldId_PlacementDrive_Text") || field.getString("fieldId_PlacementDrive_Text") == null || field.getString("fieldId_PlacementDrive_Text").trim().isEmpty()) {
                                try {
                                    Response response = redisAPI.incr("field:fieldId:counter")
                                            .toCompletionStage()
                                            .toCompletableFuture()
                                            .get(5, java.util.concurrent.TimeUnit.SECONDS);
                                    field.put("fieldId_PlacementDrive_Text", String.format("F%03d", response.toLong()));
                                } catch (Exception e) {
                                    field.put("fieldId_PlacementDrive_Text", "F-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                                }
                            }
                        }
                    }
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
                return result.put("error", "Unable to create placement");
            }
        } catch (Exception e) {
            return result.put("error", "Unable to create placement: " + e.getMessage());
        }
    }

    public Document updatePlacement(String id, Document updateDoc) {
        if (updateDoc.isEmpty()) {
            throw new IllegalArgumentException("No fields provided for update");
        }

        Document updateObj = new Document("$set", updateDoc);
        Bson filter = buildQuery(id);

        try (ClientSession clientSession = getMongoDbSession(mongoClient)) {
            startTransaction(clientSession);
            FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);

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

    public boolean deletePlacement(String id) {
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

    // ── Job operations ─────────────────────────────────────────────────────────

    public Document addJob(String placementId, Document jobDoc) {
        if (!jobDoc.containsKey("jobId_PlacementDrive_Text") || jobDoc.getString("jobId_PlacementDrive_Text") == null || jobDoc.getString("jobId_PlacementDrive_Text").trim().isEmpty()) {
            jobDoc.put("jobId_PlacementDrive_Text", "J-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }

        Bson filter = buildQuery(placementId);
        Bson update = Updates.push("jobs_PlacementDrive_DocumentArray", jobDoc);

        try (ClientSession clientSession = getMongoDbSession(mongoClient)) {
            startTransaction(clientSession);
            FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);

            Document updatedDoc = mongoDatabase.getCollection(COLLECTION).findOneAndUpdate(clientSession, filter, update, options);
            if (updatedDoc != null) {
                commitTransaction(clientSession);
                return getPlacementById(placementId); // Ensure response format is correct
            } else {
                abortTransaction(clientSession);
                return null;
            }
        }
    }

    public Document updateJob(String placementId, String jobId, Document fieldsToUpdate) {
        Bson filter = Filters.and(buildQuery(placementId), Filters.eq("jobs_PlacementDrive_DocumentArray.jobId_PlacementDrive_Text", jobId));

        List<Bson> updates = new ArrayList<>();
        for (String key : fieldsToUpdate.keySet()) {
            if (!key.equals("jobId_PlacementDrive_Text")) {
                updates.add(Updates.set("jobs_PlacementDrive_DocumentArray.$." + key, fieldsToUpdate.get(key)));
            }
        }

        if (updates.isEmpty()) return getPlacementById(placementId);

        Bson update = Updates.combine(updates);

        try (ClientSession clientSession = getMongoDbSession(mongoClient)) {
            startTransaction(clientSession);
            FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);

            Document updatedDoc = mongoDatabase.getCollection(COLLECTION).findOneAndUpdate(clientSession, filter, update, options);
            if (updatedDoc != null) {
                commitTransaction(clientSession);
                return getPlacementById(placementId);
            } else {
                abortTransaction(clientSession);
                return null;
            }
        }
    }

    public Document deleteJob(String placementId, String jobId) {
        Bson filter = buildQuery(placementId);
        Bson update = Updates.pull("jobs_PlacementDrive_DocumentArray", new Document("jobId_PlacementDrive_Text", jobId));

        try (ClientSession clientSession = getMongoDbSession(mongoClient)) {
            startTransaction(clientSession);
            FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);

            Document updatedDoc = mongoDatabase.getCollection(COLLECTION).findOneAndUpdate(clientSession, filter, update, options);
            if (updatedDoc != null) {
                commitTransaction(clientSession);
                return getPlacementById(placementId);
            } else {
                abortTransaction(clientSession);
                return null;
            }
        }
    }

    // ── Job Field operations ───────────────────────────────────────────────────

    public Document addJobField(String placementId, String jobId, Document fieldDoc) {
        if (!fieldDoc.containsKey("fieldId_PlacementDrive_Text") || fieldDoc.getString("fieldId_PlacementDrive_Text") == null || fieldDoc.getString("fieldId_PlacementDrive_Text").trim().isEmpty()) {
            fieldDoc.put("fieldId_PlacementDrive_Text", "F-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }

        Bson filter = Filters.and(buildQuery(placementId), Filters.eq("jobs_PlacementDrive_DocumentArray.jobId_PlacementDrive_Text", jobId));
        Bson update = Updates.push("jobs_PlacementDrive_DocumentArray.$.fields_PlacementDrive_DocumentArray", fieldDoc);

        try (ClientSession clientSession = getMongoDbSession(mongoClient)) {
            startTransaction(clientSession);
            FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);

            Document updatedDoc = mongoDatabase.getCollection(COLLECTION).findOneAndUpdate(clientSession, filter, update, options);
            if (updatedDoc != null) {
                commitTransaction(clientSession);
                return getPlacementById(placementId);
            } else {
                abortTransaction(clientSession);
                return null;
            }
        }
    }

    public Document updateJobField(String placementId, String jobId, String fieldId, Document fieldUpdates) {
        // Direct update of nested array element (arrayFilters)
        Bson filter = buildQuery(placementId);
        
        List<Bson> setUpdates = new ArrayList<>();
        for (String key : fieldUpdates.keySet()) {
            if (!key.equals("fieldId_PlacementDrive_Text")) {
                setUpdates.add(Updates.set("jobs_PlacementDrive_DocumentArray.$[job].fields_PlacementDrive_DocumentArray.$[field]." + key, fieldUpdates.get(key)));
            }
        }
        
        if (setUpdates.isEmpty()) return getPlacementById(placementId);

        Bson update = Updates.combine(setUpdates);
        
        List<Bson> arrayFilters = new ArrayList<>();
        arrayFilters.add(Filters.eq("job.jobId_PlacementDrive_Text", jobId));
        arrayFilters.add(Filters.eq("field.fieldId_PlacementDrive_Text", fieldId));

        try (ClientSession clientSession = getMongoDbSession(mongoClient)) {
            startTransaction(clientSession);
            FindOneAndUpdateOptions options = new FindOneAndUpdateOptions()
                    .returnDocument(ReturnDocument.AFTER)
                    .arrayFilters(arrayFilters);

            Document updatedDoc = mongoDatabase.getCollection(COLLECTION).findOneAndUpdate(clientSession, filter, update, options);
            if (updatedDoc != null) {
                commitTransaction(clientSession);
                return getPlacementById(placementId);
            } else {
                abortTransaction(clientSession);
                return null;
            }
        }
    }

    public Document deleteJobField(String placementId, String jobId, String fieldId) {
        Bson filter = Filters.and(buildQuery(placementId), Filters.eq("jobs_PlacementDrive_DocumentArray.jobId_PlacementDrive_Text", jobId));
        Bson update = Updates.pull("jobs_PlacementDrive_DocumentArray.$.fields_PlacementDrive_DocumentArray", new Document("fieldId_PlacementDrive_Text", fieldId));

        try (ClientSession clientSession = getMongoDbSession(mongoClient)) {
            startTransaction(clientSession);
            FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER);

            Document updatedDoc = mongoDatabase.getCollection(COLLECTION).findOneAndUpdate(clientSession, filter, update, options);
            if (updatedDoc != null) {
                commitTransaction(clientSession);
                return getPlacementById(placementId);
            } else {
                abortTransaction(clientSession);
                return null;
            }
        }
    }
}
