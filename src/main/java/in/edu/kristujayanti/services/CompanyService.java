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

        List<Document> allCompaniesList = findDocuments(mongoDatabase, "companies").projection(Projections.fields(Projections.excludeId())).into(new ArrayList<>());

        for(Document companyDoc : allCompaniesList){
            ResponseUtil.processResponseDocumentWithoutZone(companyDoc);
        }

        return allCompaniesList;
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

}
