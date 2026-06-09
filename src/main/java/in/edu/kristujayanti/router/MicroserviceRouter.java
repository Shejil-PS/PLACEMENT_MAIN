package in.edu.kristujayanti.router;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import in.edu.kristujayanti.constants.CommonKeys;
import in.edu.kristujayanti.constants.ContextRoutingURLName;
import in.edu.kristujayanti.constants.MicroserviceRoutingURLNames;
import in.edu.kristujayanti.handlers.companyManagement.CreateCompanyHandler;
import in.edu.kristujayanti.handlers.companyManagement.DeleteCompanyHandler;
import in.edu.kristujayanti.handlers.companyManagement.GetCompanyByIdHandler;
import in.edu.kristujayanti.handlers.companyManagement.ListCompaniesHandler;
import in.edu.kristujayanti.handlers.companyManagement.UpdateCompanyHandler;
import in.edu.kristujayanti.handlers.studentManagement.CreateStudentHandler;
import in.edu.kristujayanti.handlers.studentManagement.DeleteStudentHandler;
import in.edu.kristujayanti.handlers.studentManagement.GetStudentByIdHandler;
import in.edu.kristujayanti.handlers.studentManagement.GetStudentsHandler;
import in.edu.kristujayanti.handlers.studentManagement.UpdateStudentHandler;
import in.edu.kristujayanti.handlers.batchManagement.CreateBatchHandler;
import in.edu.kristujayanti.handlers.batchManagement.DeleteBatchHandler;
import in.edu.kristujayanti.handlers.batchManagement.GetBatchByIdHandler;
import in.edu.kristujayanti.handlers.batchManagement.GetBatchesHandler;
import in.edu.kristujayanti.handlers.batchManagement.UpdateBatchHandler;
import in.edu.kristujayanti.handlers.applicationManagement.ApplyForJobHandler;
import in.edu.kristujayanti.handlers.applicationManagement.ExportApplicationsHandler;
import in.edu.kristujayanti.handlers.applicationManagement.GetApplicationByIdHandler;
import in.edu.kristujayanti.handlers.applicationManagement.GetApplicationsHandler;
import in.edu.kristujayanti.handlers.applicationManagement.UpdateApplicationStatusHandler;
import in.edu.kristujayanti.services.CompanyService;
import in.edu.kristujayanti.services.StudentService;
import in.edu.kristujayanti.services.BatchService;
import in.edu.kristujayanti.services.ApplicationService;
import in.edu.kristujayanti.services.HealthService;
import in.edu.kristujayanti.handlers.HealthHandler;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.redis.client.Redis;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * MicroserviceRouter sets up the main application routes and handlers.
 * It extends the RouterBase to utilize common properties and methods for
 * routing.
 */
public class MicroserviceRouter extends RouterBase {

        /**
         * Constructs a ReportOrchestratorRouter with necessary dependencies.
         *
         * @param router                 the Vert.x router
         * @param redisCommandConnection the Redis connection
         * @param mongoDatabase          the MongoDB database
         * @param mongoClient            the MongoDB client
         * @param client                 the Vert.x WebClient
         */
        public MicroserviceRouter(Router router, Redis redisCommandConnection, MongoDatabase mongoDatabase,
                        MongoClient mongoClient, WebClient client, JsonObject apiInfo, Vertx vertx) {
                super(router, redisCommandConnection, mongoDatabase, mongoClient, client, apiInfo, vertx);
        }

        /**
         * Set up the application routes with handlers.
         */
        public void setUpRouters() {
                // Define allowed headers for CORS
                Set<String> allowHeaders = Stream.of(
                                CommonKeys.CONTENT_TYPE,
                                CommonKeys.X_AUTH_CORRELATION_ID,
                                HttpHeaders.AUTHORIZATION.toString(),
                                "Access-Control-Allow-Origin").collect(Collectors.toSet());

                // Define allowed HTTP methods for CORS
                Set<HttpMethod> allowMethods = Stream.of(
                                HttpMethod.GET,
                                HttpMethod.POST,
                                HttpMethod.PUT,
                                HttpMethod.DELETE,
                                HttpMethod.PATCH).collect(Collectors.toSet());

                // Setup CORS and Body Handlers
                this.router.route().handler(CorsHandler.create()
                                .addOrigin("*")
                                .allowCredentials(true)
                                .allowedHeaders(allowHeaders)
                                .allowedMethods(allowMethods));

                // add routes here
                // health route
                HealthService healthService = new HealthService(this.mongoDatabase);
                addRoute(HttpMethod.GET, MicroserviceRoutingURLNames.HEALTH_URL, new HealthHandler(healthService));

                CompanyService companyService = new CompanyService(mongoDatabase, mongoClient);

                addRoute(HttpMethod.GET, MicroserviceRoutingURLNames.LIST_COMPANIES, new ListCompaniesHandler(companyService));

                addRoute(HttpMethod.POST, MicroserviceRoutingURLNames.CREATE_COMPANY, new CreateCompanyHandler(companyService));

                addRoute(HttpMethod.GET, MicroserviceRoutingURLNames.GET_COMPANY_BY_ID, new GetCompanyByIdHandler(companyService));

                addRoute(HttpMethod.PUT, MicroserviceRoutingURLNames.UPDATE_COMPANY, new UpdateCompanyHandler(companyService));

                addRoute(HttpMethod.DELETE, MicroserviceRoutingURLNames.DELETE_COMPANY, new DeleteCompanyHandler(companyService));

                // Student Management
                StudentService studentService = new StudentService(mongoDatabase, mongoClient);

                addRoute(HttpMethod.POST, MicroserviceRoutingURLNames.CREATE_STUDENT, new CreateStudentHandler(studentService));

                addRoute(HttpMethod.GET, MicroserviceRoutingURLNames.LIST_STUDENTS, new GetStudentsHandler(studentService));

                addRoute(HttpMethod.GET, MicroserviceRoutingURLNames.GET_STUDENT_BY_ID, new GetStudentByIdHandler(studentService));

                addRoute(HttpMethod.PUT, MicroserviceRoutingURLNames.UPDATE_STUDENT, new UpdateStudentHandler(studentService));

                addRoute(HttpMethod.DELETE, MicroserviceRoutingURLNames.DELETE_STUDENT, new DeleteStudentHandler(studentService));

                // Batch Management
                BatchService batchService = new BatchService(mongoDatabase, mongoClient, redisCommandConnection);

                addRoute(HttpMethod.POST, MicroserviceRoutingURLNames.CREATE_BATCH, new CreateBatchHandler(batchService));

                addRoute(HttpMethod.GET, MicroserviceRoutingURLNames.LIST_BATCHES, new GetBatchesHandler(batchService));

                addRoute(HttpMethod.GET, MicroserviceRoutingURLNames.GET_BATCH_BY_ID, new GetBatchByIdHandler(batchService));

                addRoute(HttpMethod.PUT, MicroserviceRoutingURLNames.UPDATE_BATCH, new UpdateBatchHandler(batchService));

                addRoute(HttpMethod.DELETE, MicroserviceRoutingURLNames.DELETE_BATCH, new DeleteBatchHandler(batchService));

                // Application Management
                ApplicationService applicationService = new ApplicationService(mongoDatabase, mongoClient, redisCommandConnection);

                addRoute(HttpMethod.POST, MicroserviceRoutingURLNames.APPLY_FOR_JOB, new ApplyForJobHandler(applicationService));

                addRoute(HttpMethod.GET, MicroserviceRoutingURLNames.LIST_APPLICATIONS, new GetApplicationsHandler(applicationService));

                // Export route must be registered before parameterized route to avoid parameter matching collision
                addRoute(HttpMethod.GET, MicroserviceRoutingURLNames.EXPORT_APPLICATIONS, new ExportApplicationsHandler(applicationService));

                addRoute(HttpMethod.GET, MicroserviceRoutingURLNames.GET_APPLICATION_BY_ID, new GetApplicationByIdHandler(applicationService));

                addRoute(HttpMethod.PATCH, MicroserviceRoutingURLNames.UPDATE_APPLICATION_STATUS, new UpdateApplicationStatusHandler(applicationService));

        }

        /**
         * Helper method to add routes with the specified method, path, and handler.
         *
         * @param method  the HTTP method
         * @param path    the URL path
         * @param handler the request handler
         */
        private void addRoute(HttpMethod method, String path, Handler<RoutingContext> handler) {
                this.router.route(method, ContextRoutingURLName.MICROSERVICE_CONTEXT_URL_NAME.concat(path))
                                .handler(BodyHandler.create())
                                .blockingHandler(handler);
        }
}
