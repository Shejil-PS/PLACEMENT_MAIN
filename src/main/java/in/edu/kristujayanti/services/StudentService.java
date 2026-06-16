package in.edu.kristujayanti.services;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import in.edu.kristujayanti.dbaccess.MongoDataAccess;
import in.edu.kristujayanti.util.ResponseUtil;
import io.vertx.core.json.JsonObject;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class StudentService extends MongoDataAccess {

    private final MongoDatabase mongoDatabase;
    private final MongoClient mongoClient;
    private static final String COLLECTION = "students";

    public StudentService(MongoDatabase mongoDatabase, MongoClient mongoClient) {
        this.mongoDatabase = mongoDatabase;
        this.mongoClient = mongoClient;
    }

    public List<Document> getAllStudents() {
        return getAllStudents(null);
    }

    public List<Document> getAllStudents(String search) {
        org.bson.conversions.Bson filter = new Document();
        if (search != null && !search.trim().isEmpty()) {
            java.util.regex.Pattern regex = java.util.regex.Pattern.compile(search, java.util.regex.Pattern.CASE_INSENSITIVE);
            List<org.bson.conversions.Bson> orConditions = new ArrayList<>();
            orConditions.add(com.mongodb.client.model.Filters.regex("firstName_PlacementStudent_Text", regex));
            orConditions.add(com.mongodb.client.model.Filters.regex("lastName_PlacementStudent_Text", regex));
            orConditions.add(com.mongodb.client.model.Filters.regex("rollNo_PlacementStudent_Text", regex));
            orConditions.add(com.mongodb.client.model.Filters.regex("departmentName_PlacementStudent_Text", regex));
            orConditions.add(com.mongodb.client.model.Filters.regex("specialization_PlacementStudent_Text", regex));
            orConditions.add(com.mongodb.client.model.Filters.regex("course", regex));
            
            String s = search.toLowerCase().trim();
            if (s.contains("opted in")) {
                orConditions.add(com.mongodb.client.model.Filters.eq("optedIn_PlacementStudent_Bool", true));
                orConditions.add(com.mongodb.client.model.Filters.eq("optInStatus", "opted_in"));
                orConditions.add(com.mongodb.client.model.Filters.eq("optedIn", true));
            } else if (s.contains("pending")) {
                orConditions.add(com.mongodb.client.model.Filters.and(
                    com.mongodb.client.model.Filters.ne("optedIn_PlacementStudent_Bool", true),
                    com.mongodb.client.model.Filters.ne("optedIn_PlacementStudent_Bool", false)
                ));
                orConditions.add(com.mongodb.client.model.Filters.eq("optInStatus", "pending"));
            } else if (s.contains("opted out")) {
                orConditions.add(com.mongodb.client.model.Filters.eq("optedIn_PlacementStudent_Bool", false));
                orConditions.add(com.mongodb.client.model.Filters.eq("optedIn", false));
                orConditions.add(com.mongodb.client.model.Filters.eq("optInStatus", "opted_out"));
            }

            if (s.contains("frozen")) {
                orConditions.add(com.mongodb.client.model.Filters.eq("freeze_PlacementStudent_Bool", true));
            } else if (s.contains("active")) {
                orConditions.add(com.mongodb.client.model.Filters.ne("freeze_PlacementStudent_Bool", true));
            }

            if (s.equals("placed")) {
                orConditions.add(com.mongodb.client.model.Filters.eq("isPlaced_PlacementStudent_Bool", true));
                orConditions.add(com.mongodb.client.model.Filters.eq("isPlaced", true));
                orConditions.add(com.mongodb.client.model.Filters.eq("placedStatus_PlacementStudent_Bool", true));
            } else if (s.equals("unplaced")) {
                orConditions.add(com.mongodb.client.model.Filters.ne("isPlaced_PlacementStudent_Bool", true));
                orConditions.add(com.mongodb.client.model.Filters.ne("isPlaced", true));
                orConditions.add(com.mongodb.client.model.Filters.ne("placedStatus_PlacementStudent_Bool", true));
            }

            try {
                double val = Double.parseDouble(search);
                orConditions.add(com.mongodb.client.model.Filters.eq("cgpa_PlacementStudent_Double", val));
                orConditions.add(com.mongodb.client.model.Filters.eq("cgpa", val));
            } catch (NumberFormatException e) {
                // Not a number, ignore
            }

            filter = com.mongodb.client.model.Filters.or(orConditions);
        }
        List<Document> studentsList = findDocumentsWithFilter(mongoDatabase, COLLECTION, filter).into(new ArrayList<>());
        for (Document doc : studentsList) {
            Object originalId = doc.get("_id");
            if (originalId instanceof String) {
                doc.remove("_id");
            }
            ResponseUtil.processResponseDocumentWithoutZone(doc);
            if (originalId instanceof String) {
                doc.put("_id", originalId);
            }
        }
        return studentsList;
    }

    public Document getStudentById(String id) {
        if (org.bson.types.ObjectId.isValid(id)) {
            Document doc = findSingleDocument(mongoDatabase, COLLECTION, com.mongodb.client.model.Filters.eq("_id", new org.bson.types.ObjectId(id)));
            if (doc != null) {
                return doc;
            }
        }
        return findSingleDocument(mongoDatabase, COLLECTION, com.mongodb.client.model.Filters.eq("_id", id));
    }

    public JsonObject createStudent(Document paramsDoc) {
        JsonObject result = new JsonObject();
        try (ClientSession clientSession = getMongoDbSession(mongoClient)) {
            startTransaction(clientSession);
            boolean saved = saveDocument(COLLECTION, paramsDoc, clientSession, mongoDatabase);
            if (saved) {
                commitTransaction(clientSession);
                return result;
            } else {
                abortTransaction(clientSession);
                return result.put("error", "Unable to create student");
            }
        } catch (Exception e) {
            return result.put("error", "Unable to create student: " + e.getMessage());
        }
    }

    public Document updateStudent(String id, Document updateDoc) {
        if (updateDoc.isEmpty()) {
            throw new IllegalArgumentException("No fields provided for update");
        }

        Document updateObj = new Document("$set", updateDoc);

        org.bson.conversions.Bson filter;
        if (org.bson.types.ObjectId.isValid(id)) {
            filter = com.mongodb.client.model.Filters.eq("_id", new org.bson.types.ObjectId(id));
        } else {
            filter = com.mongodb.client.model.Filters.eq("_id", id);
        }

        try (ClientSession clientSession = getMongoDbSession(mongoClient)) {
            startTransaction(clientSession);
            com.mongodb.client.model.FindOneAndUpdateOptions options = new com.mongodb.client.model.FindOneAndUpdateOptions()
                    .returnDocument(com.mongodb.client.model.ReturnDocument.AFTER);

            Document updatedDoc = mongoDatabase.getCollection(COLLECTION).findOneAndUpdate(clientSession, filter, updateObj, options);
            if (updatedDoc != null) {
                commitTransaction(clientSession);
                return updatedDoc;
            } else {
                abortTransaction(clientSession);
                return null;
            }
        }
    }

    public boolean deleteStudent(String id) {
        org.bson.conversions.Bson filter;
        if (org.bson.types.ObjectId.isValid(id)) {
            filter = com.mongodb.client.model.Filters.eq("_id", new org.bson.types.ObjectId(id));
        } else {
            filter = com.mongodb.client.model.Filters.eq("_id", id);
        }

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
