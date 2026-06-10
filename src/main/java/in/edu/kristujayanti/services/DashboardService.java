package in.edu.kristujayanti.services;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
import in.edu.kristujayanti.dbaccess.MongoDataAccess;
import in.edu.kristujayanti.util.ResponseUtil;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DashboardService extends MongoDataAccess {

    private final MongoDatabase mongoDatabase;

    public DashboardService(MongoDatabase mongoDatabase) {
        this.mongoDatabase = mongoDatabase;
    }

    public JsonObject getSummary() {
        long studentCount = mongoDatabase.getCollection("students").countDocuments();
        long batchCount = mongoDatabase.getCollection("batches").countDocuments();
        long companyCount = mongoDatabase.getCollection("companies").countDocuments();
        long placementCount = mongoDatabase.getCollection("placements").countDocuments();
        long applicationCount = mongoDatabase.getCollection("applications").countDocuments();

        return new JsonObject()
                .put("totalStudents", studentCount)
                .put("totalBatches", batchCount)
                .put("totalCompanies", companyCount)
                .put("totalPlacements", placementCount)
                .put("totalApplications", applicationCount);
    }

    public JsonArray getPlacementStats() {
        MongoCollection<Document> collection = mongoDatabase.getCollection("applications");

        Document groupStage = Document.parse("{ $group: { _id: '$placementId', totalApplications: { $sum: 1 }, statusBreakdown: { $push: { status: '$status' } } } }");
        Document sortStage = Document.parse("{ $sort: { totalApplications: -1 } }");

        List<Document> results = collection.aggregate(Arrays.asList(groupStage, sortStage)).into(new ArrayList<>());
        JsonArray array = new JsonArray();
        for (Document doc : results) {
            array.add(new JsonObject(doc.toJson()));
        }
        return array;
    }

    public JsonArray getRecentActivity() {
        MongoCollection<Document> collection = mongoDatabase.getCollection("applications");
        // Sort by _id descending to get the most recent documents (ObjectId contains timestamp)
        List<Document> results = collection.find().sort(Sorts.descending("_id")).limit(10).into(new ArrayList<>());
        
        JsonArray array = new JsonArray();
        for (Document doc : results) {
            Object originalId = doc.get("_id");
            if (originalId instanceof String) {
                doc.remove("_id");
            }
            ResponseUtil.processResponseDocumentWithoutZone(doc);
            if (originalId instanceof String) {
                doc.put("_id", originalId);
            }
            array.add(new JsonObject(doc.toJson()));
        }
        return array;
    }
}
