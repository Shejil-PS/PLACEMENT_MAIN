package in.edu.kristujayanti.services;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Projections;
import in.edu.kristujayanti.collectionNames.PlacementsCNBinder;
import in.edu.kristujayanti.dbaccess.MongoDataAccess;
import in.edu.kristujayanti.util.ResponseUtil;
import io.vertx.core.json.JsonObject;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class CompanyService extends MongoDataAccess {

    private final MongoDatabase mongoDatabase;

    private final MongoClient mongoClient;

//    private static String PLACEMENTS_COMPANY_COLLECTION = PlacementsCNBinder.PLACEMENTS_COMPANY_COLLECTION.getCollectionName();

    public CompanyService(MongoDatabase mongoDatabase, MongoClient mongoClient) {
        this.mongoDatabase = mongoDatabase;
        this.mongoClient = mongoClient;
    }


    public List<Document> getAllCompanies(){

        List<Document> allCompaniesList = findDocuments(mongoDatabase, "companies").into(new ArrayList<>());

        for(Document companyDoc : allCompaniesList){
            ResponseUtil.processResponseDocumentWithoutZone(companyDoc);
        }

        return allCompaniesList;
    }

    public Document getCompanyById(String id) {
        if (org.bson.types.ObjectId.isValid(id)) {
            Document doc = findSingleDocument(mongoDatabase, "companies", com.mongodb.client.model.Filters.eq("_id", new org.bson.types.ObjectId(id)));
            if (doc != null) {
                return doc;
            }
        }
        return findSingleDocument(mongoDatabase, "companies", com.mongodb.client.model.Filters.eq("_id", id));
    }

    //Create Company
    public JsonObject createCompany(Document paramsDoc){
        JsonObject result = new JsonObject();
        try(ClientSession clientSession = getMongoDbSession(mongoClient)){
            startTransaction(clientSession);

            //Add Company code creation Logic
            boolean saveDoc = saveDocument("companies", paramsDoc, clientSession, mongoDatabase);

            if(saveDoc){
                commitTransaction(clientSession);
                return result;
            }else{
                abortTransaction(clientSession);
                return result.put("error", "Unable To Create Company");
            }

        }catch (Exception e){
            return result.put("error", "Unable to create company "+ e.getMessage());
        }
    }

    public Document updateCompany(String id, Document updateDoc) {
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

            Document updatedDoc = mongoDatabase.getCollection("companies").findOneAndUpdate(clientSession, filter, updateObj, options);
            if (updatedDoc != null) {
                commitTransaction(clientSession);
                return updatedDoc;
            } else {
                abortTransaction(clientSession);
                return null;
            }
        }
    }

    public boolean deleteCompany(String id) {
        org.bson.conversions.Bson filter;
        if (org.bson.types.ObjectId.isValid(id)) {
            filter = com.mongodb.client.model.Filters.eq("_id", new org.bson.types.ObjectId(id));
        } else {
            filter = com.mongodb.client.model.Filters.eq("_id", id);
        }

        try (ClientSession clientSession = getMongoDbSession(mongoClient)) {
            startTransaction(clientSession);
            boolean deleted = deleteDocument("companies", filter, clientSession, mongoDatabase);
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
