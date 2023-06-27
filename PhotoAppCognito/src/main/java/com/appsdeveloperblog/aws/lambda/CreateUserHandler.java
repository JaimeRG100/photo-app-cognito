package com.appsdeveloperblog.aws.lambda;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.appsdeveloperblog.aws.lambda.service.CognitoUserService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

public class CreateUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final CognitoUserService cognitoUserService;
    private final String appClientId;
    private final String appClientSecret;

    // Junit Section
//    public CreateUserHandler(CognitoUserService cognitoUserService,
//            String appClientId,
//            String appClientSecret) {
//        this.cognitoUserService = cognitoUserService;
//        this.appClientId = appClientId;
//        this.appClientSecret = appClientSecret;
//    }

    public CreateUserHandler() {
        //System.out.println("AWS_REGION: " + System.getenv("AWS_REGION"));
        cognitoUserService = new CognitoUserService(System.getenv("AWS_REGION"));
        //appClientId = System.getenv("COGNITO_POOL_APP_CLIENT_ID");
        appClientId = Utils.decryptKey("COGNITO_POOL_APP_CLIENT_ID");
        //appClientSecret = System.getenv("COGNITO_POOL_APP_CLIENT_SECRET");
        appClientSecret = Utils.decryptKey("COGNITO_POOL_APP_CLIENT_SECRET");
    }

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent request, final Context context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(headers);

        String requestBody = request.getBody();
        LambdaLogger logger = context.getLogger();
        logger.log("Original json body: " + requestBody);

        JsonObject userDetails = null;

        try {
            userDetails = JsonParser.parseString(requestBody).getAsJsonObject();
            JsonObject createUserResult = cognitoUserService.createUser(userDetails, appClientId, appClientSecret);
            response.withStatusCode(200);
            response.withBody(new Gson().toJson(createUserResult, JsonObject.class));
        } catch (AwsServiceException ex) {
            logger.log("AwsServiceException in CreateUserHandler class: " + ex.awsErrorDetails().errorMessage());
            //String error = ex.awsErrorDetails().errorMessage();
            ErrorResponse errorResponse = new ErrorResponse(ex.awsErrorDetails().errorMessage(), ex.awsErrorDetails().sdkHttpResponse().statusCode());
            String errorResponseJsonString = new GsonBuilder().serializeNulls().create().toJson(errorResponse, ErrorResponse.class);
            return response
                    .withStatusCode(ex.awsErrorDetails().sdkHttpResponse().statusCode())
                    //.withBody("{\"error\": \"" + error.replaceAll("\"", "\\\\\"") + "\"}");
                    .withBody(errorResponseJsonString);
        } catch (RuntimeException ex) {
            logger.log("RuntimeException in CreateUserHandler class: " + ex.getMessage());
            ErrorResponse errorResponse = new ErrorResponse(ex.getMessage(), 500);
            String errorResponseJsonString = new GsonBuilder().serializeNulls().create().toJson(errorResponse, ErrorResponse.class);
            return response
                    .withStatusCode(500)
                    .withBody(errorResponseJsonString);
        }

        return response;
    }

}
