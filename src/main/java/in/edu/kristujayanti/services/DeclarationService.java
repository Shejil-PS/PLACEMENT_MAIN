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

public class DeclarationService extends MongoDataAccess {

    private final MongoDatabase mongoDatabase;
    private final MongoClient mongoClient;

    public DeclarationService(MongoDatabase mongoDatabase, MongoClient mongoClient) {
        this.mongoDatabase = mongoDatabase;
        this.mongoClient = mongoClient;
    }

    public List<Document> getAllDeclarations() {
        return getAllDeclarations(null);
    }

    public List<Document> getAllDeclarations(String search){
        org.bson.conversions.Bson filter = new Document();
        if (search != null && !search.trim().isEmpty()) {
            java.util.regex.Pattern regex = java.util.regex.Pattern.compile(search, java.util.regex.Pattern.CASE_INSENSITIVE);
            filter = com.mongodb.client.model.Filters.regex("declarationForm_PlacementDeclare_Text", regex);
        }

        List<Document> allDeclarationsList = findDocumentsWithFilter(mongoDatabase, "declarations", filter).into(new ArrayList<>());

        for(Document declarationDoc : allDeclarationsList){
            ResponseUtil.processResponseDocumentWithoutZone(declarationDoc);
        }

        return allDeclarationsList;
    }

    public Document getDeclarationById(String id) {
        if (org.bson.types.ObjectId.isValid(id)) {
            Document doc = findSingleDocument(mongoDatabase, "declarations", com.mongodb.client.model.Filters.eq("_id", new org.bson.types.ObjectId(id)));
            if (doc != null) {
                return doc;
            }
        }
        return findSingleDocument(mongoDatabase, "declarations", com.mongodb.client.model.Filters.eq("_id", id));
    }

    //Create Declaration
    public JsonObject createDeclaration(Document paramsDoc){
        JsonObject result = new JsonObject();
        try(ClientSession clientSession = getMongoDbSession(mongoClient)){
            startTransaction(clientSession);

            boolean saveDoc = saveDocument("declarations", paramsDoc, clientSession, mongoDatabase);

            if(saveDoc){
                commitTransaction(clientSession);
                return result;
            }else{
                abortTransaction(clientSession);
                return result.put("error", "Unable To Create Declaration");
            }

        }catch (Exception e){
            return result.put("error", "Unable to create declaration "+ e.getMessage());
        }
    }

    public Document updateDeclaration(String id, Document updateDoc) {
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

            Document updatedDoc = mongoDatabase.getCollection("declarations").findOneAndUpdate(clientSession, filter, updateObj, options);
            if (updatedDoc != null) {
                commitTransaction(clientSession);
                return updatedDoc;
            } else {
                abortTransaction(clientSession);
                return null;
            }
        }
    }

    public boolean deleteDeclaration(String id) {
        org.bson.conversions.Bson filter;
        if (org.bson.types.ObjectId.isValid(id)) {
            filter = com.mongodb.client.model.Filters.eq("_id", new org.bson.types.ObjectId(id));
        } else {
            filter = com.mongodb.client.model.Filters.eq("_id", id);
        }

        try (ClientSession clientSession = getMongoDbSession(mongoClient)) {
            startTransaction(clientSession);
            boolean deleted = deleteDocument("declarations", filter, clientSession, mongoDatabase);
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
