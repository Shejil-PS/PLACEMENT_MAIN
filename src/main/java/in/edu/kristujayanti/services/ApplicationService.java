package in.edu.kristujayanti.services;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import in.edu.kristujayanti.dbaccess.MongoDataAccess;
import in.edu.kristujayanti.util.ResponseUtil;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.Response;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

public class ApplicationService extends MongoDataAccess {

    private final MongoDatabase mongoDatabase;
    private final MongoClient mongoClient;
    private final Redis redisClient;
    private static final String COLLECTION = "applications";

    private static final List<String> VALID_STATUSES = List.of(
            "Applied", "Shortlisted", "Interview Scheduled",
            "Interview Completed", "Selected", "Rejected", "On Hold"
    );

    public ApplicationService(MongoDatabase mongoDatabase, MongoClient mongoClient, Redis redisClient) {
        this.mongoDatabase = mongoDatabase;
        this.mongoClient = mongoClient;
        this.redisClient = redisClient;
    }

    public JsonObject applyForJob(Document paramsDoc) {
        String studentId = paramsDoc.getString("studentId");
        String jobId = paramsDoc.getString("jobId");

        Bson duplicateFilter = Filters.and(
                Filters.eq("studentId", studentId),
                Filters.eq("jobId", jobId)
        );

        Document existing = findSingleDocument(mongoDatabase, COLLECTION, duplicateFilter);
        if (existing != null) {
            return new JsonObject().put("error", "Student " + studentId + " has already applied for job " + jobId);
        }

        try (ClientSession clientSession = getMongoDbSession(mongoClient)) {
            if (!paramsDoc.containsKey("applicationId") || paramsDoc.getString("applicationId") == null || paramsDoc.getString("applicationId").trim().isEmpty()) {
                try {
                    RedisAPI redisAPI = RedisAPI.api(redisClient);
                    Response response = redisAPI.incr("placement:applicationId:counter")
                            .toCompletionStage()
                            .toCompletableFuture()
                            .get(5, java.util.concurrent.TimeUnit.SECONDS);
                    long counter = response.toLong();
                    String newAppId = String.format("A%03d", counter);
                    paramsDoc.put("applicationId", newAppId);
                    paramsDoc.put("applicationId_PlacementAppilcation_Text", newAppId);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to generate applicationId from Redis: " + e.getMessage());
                }
            }

            if (!paramsDoc.containsKey("status") || paramsDoc.getString("status") == null || paramsDoc.getString("status").trim().isEmpty()) {
                paramsDoc.put("status", "Applied");
                paramsDoc.put("status_PlacementAppilcation_Text", "Applied");
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
                return new JsonObject().put("error", "Unable to create application");
            }
        } catch (Exception e) {
            return new JsonObject().put("error", "Unable to create application: " + e.getMessage());
        }
    }

    public List<Document> getAllApplications(JsonObject filters) {
        String placementId = filters.getString("placementId");
        Bson query;

        if (placementId == null) {
            query = buildFilterQuery(filters);
        } else {
            String searchPlacementId = placementId;
            int jobIndex = -1;
            if (placementId.contains("_")) {
                String[] parts = placementId.split("_");
                searchPlacementId = parts[0];
                try {
                    jobIndex = Integer.parseInt(parts[1]);
                } catch (Exception e) {
                    // Ignore parsing error
                }
            }

            final String finalSearchPlacementId = searchPlacementId;
            final int finalJobIndex = jobIndex;

            Bson queryPlacements = Filters.or(
                    Filters.eq("_id", finalSearchPlacementId),
                    Filters.eq("jobs.jobId", finalSearchPlacementId)
            );

            Document placement = findSingleDocument(mongoDatabase, "placements", queryPlacements);
            JsonObject resolvedFilters = filters.copy();

            if (placement != null) {
                String realPlacementId = placement.getString("_id");
                String resolvedJobId = null;

                List<Document> jobs = placement.getList("jobs", Document.class);
                if (jobs != null) {
                    if (finalJobIndex >= 0 && finalJobIndex < jobs.size()) {
                        resolvedJobId = jobs.get(finalJobIndex).getString("jobId");
                    } else {
                        for (Document job : jobs) {
                            String jId = job.getString("jobId");
                            if (finalSearchPlacementId.equals(jId)) {
                                resolvedJobId = jId;
                                break;
                            }
                        }
                    }
                }

                resolvedFilters.put("placementId", realPlacementId);
                if (resolvedJobId != null) {
                    resolvedFilters.put("jobId", resolvedJobId);
                } else {
                    resolvedFilters.remove("jobId");
                }
                query = buildFilterQuery(resolvedFilters);
            } else {
                query = buildFilterQuery(filters);
            }
        }

        List<Document> applicationsList = findDocumentsWithFilter(mongoDatabase, COLLECTION, query).into(new ArrayList<>());
        for (Document doc : applicationsList) {
            Object originalId = doc.get("_id");
            if (originalId instanceof String) {
                doc.remove("_id");
            }
            ResponseUtil.processResponseDocumentWithoutZone(doc);
            if (originalId instanceof String) {
                doc.put("_id", originalId);
            }
        }
        return applicationsList;
    }

    public Document getApplicationById(String applicationId) {
        Bson filter = Filters.eq("applicationId", applicationId);
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

    public Document updateStatus(String applicationId, String newStatus) {
        String finalStatus = newStatus;
        for (String valid : VALID_STATUSES) {
            if (valid.equalsIgnoreCase(newStatus)) {
                finalStatus = valid;
                break;
            }
        }
        if (!VALID_STATUSES.contains(finalStatus)) {
            throw new IllegalArgumentException("Invalid status '" + newStatus + "'. Allowed: " + VALID_STATUSES);
        }

        Bson filter;
        if (ObjectId.isValid(applicationId)) {
            filter = Filters.or(Filters.eq("applicationId", applicationId), Filters.eq("_id", new ObjectId(applicationId)));
        } else {
            filter = Filters.eq("applicationId", applicationId);
        }

        Document updateObj = new Document("$set", new Document("status", finalStatus)
                .append("status_PlacementAppilcation_Text", finalStatus));

        try (ClientSession clientSession = getMongoDbSession(mongoClient)) {
            startTransaction(clientSession);
            com.mongodb.client.model.FindOneAndUpdateOptions options = new com.mongodb.client.model.FindOneAndUpdateOptions()
                    .returnDocument(com.mongodb.client.model.ReturnDocument.AFTER);

            Document updatedDoc = mongoDatabase.getCollection(COLLECTION).findOneAndUpdate(clientSession, filter, updateObj, options);
            if (updatedDoc != null) {
                if ("Selected".equalsIgnoreCase(finalStatus)) {
                    String studentId = updatedDoc.getString("studentId");
                    if (studentId == null) {
                        studentId = updatedDoc.getString("studentId_PlacementAppilcation_Text");
                    }
                    if (studentId != null) {
                        String company = updatedDoc.getString("companyName");
                        if (company == null) company = updatedDoc.getString("companyName_PlacementAppilcation_Text");
                        String role = updatedDoc.getString("jobId"); // or jobTitle
                        
                        Document studentSet = new Document("freeze", true)
                                .append("freeze_PlacementStudent_Bool", true)
                                .append("placedStatus_PlacementStudent_Bool", true);
                        if (company != null) studentSet.append("placedCompany_PlacementStudent_Text", company);
                        
                        Bson studentFilter;
                        if (ObjectId.isValid(studentId)) {
                            studentFilter = Filters.or(Filters.eq("_id", new ObjectId(studentId)), Filters.eq("_id", studentId), Filters.eq("rollNo_PlacementStudent_Text", studentId));
                        } else {
                            studentFilter = Filters.or(Filters.eq("_id", studentId), Filters.eq("rollNo_PlacementStudent_Text", studentId));
                        }
                        mongoDatabase.getCollection("students").updateOne(clientSession, studentFilter, new Document("$set", studentSet));
                    }
                }
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

    public List<Document> exportApplications(String placementId, String jobId, String status) {
        List<Bson> filterList = new ArrayList<>();
        if (placementId != null && !placementId.isBlank()) {
            filterList.add(Filters.eq("placementId", placementId));
        }
        if (jobId != null && !jobId.isBlank()) {
            filterList.add(Filters.eq("jobId", jobId));
        }
        if (status != null && !status.isBlank()) {
            filterList.add(Filters.eq("status", status));
        }

        Bson query = filterList.isEmpty() ? new Document() : Filters.and(filterList);
        List<Document> list = findDocumentsWithFilter(mongoDatabase, COLLECTION, query).into(new ArrayList<>());
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

    private Bson buildFilterQuery(JsonObject filters) {
        List<Bson> filterList = new ArrayList<>();
        if (filters != null) {
            List.of("placementId", "jobId", "companyId", "studentId", "status")
                    .forEach(key -> {
                        String val = filters.getString(key);
                        if (val != null && !val.isBlank()) {
                            filterList.add(Filters.eq(key, val));
                        }
                    });
            String search = filters.getString("search");
            if (search != null && !search.trim().isEmpty()) {
                java.util.regex.Pattern regex = java.util.regex.Pattern.compile(search, java.util.regex.Pattern.CASE_INSENSITIVE);
                filterList.add(Filters.or(
                    Filters.regex("studentName_PlacementAppilcation_Text", regex),
                    Filters.regex("appilcationId_PlacementAppilcation_Text", regex),
                    Filters.regex("companyName_PlacementAppilcation_Text", regex)
                ));
            }
        }
        return filterList.isEmpty() ? new Document() : Filters.and(filterList);
    }
}
