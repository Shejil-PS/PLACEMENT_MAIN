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
        List<Document> studentsList = findDocuments(mongoDatabase, COLLECTION).into(new ArrayList<>());
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
