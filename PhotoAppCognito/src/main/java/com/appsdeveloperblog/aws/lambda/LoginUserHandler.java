package com.appsdeveloperblog.aws.lambda;

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

import java.util.HashMap;
import java.util.Map;

public class LoginUserHandler  implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final CognitoUserService cognitoUserService;
    private final String appClientId;
    private final String appClientSecret;

    public LoginUserHandler() {
        //System.out.println("AWS_REGION: " + System.getenv("AWS_REGION"));
        cognitoUserService = new CognitoUserService(System.getenv("AWS_REGION"));
        //appClientId = System.getenv("COGNITO_POOL_APP_CLIENT_ID");
        appClientId = Utils.decryptKey("COGNITO_POOL_APP_CLIENT_ID");
        //appClientSecret = System.getenv("COGNITO_POOL_APP_CLIENT_SECRET");
        appClientSecret = Utils.decryptKey("COGNITO_POOL_APP_CLIENT_SECRET");
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        String requestBodyString = request.getBody();
        LambdaLogger logger = context.getLogger();
        try {
            JsonObject loginDetails = JsonParser.parseString(requestBodyString).getAsJsonObject();
            JsonObject loginResult = cognitoUserService.userLogin(loginDetails, appClientId, appClientSecret);
            response.withStatusCode(200);
            response.withBody(new Gson().toJson(loginResult, JsonObject.class));
        } catch (AwsServiceException ex) {
            logger.log("AwsServiceException in LoginUserHandler class: " + ex.awsErrorDetails().errorMessage());
            //String error = ex.awsErrorDetails().errorMessage();
            ErrorResponse errorResponse = new ErrorResponse(ex.awsErrorDetails().errorMessage(), ex.awsErrorDetails().sdkHttpResponse().statusCode());
            //String errorResponseJsonString = new Gson().toJson(errorResponse, ErrorResponse.class); // this will ignore null values
            String errorResponseJsonString = new GsonBuilder().serializeNulls().create().toJson(errorResponse, ErrorResponse.class);
            return response
                    .withStatusCode(ex.awsErrorDetails().sdkHttpResponse().statusCode())
                    //.withBody("{\"error\": \"" + error.replaceAll("\"", "\\\\\"") + "\"}");
                    .withBody(errorResponseJsonString);
        } catch (RuntimeException ex) {
            logger.log("RuntimeException in LoginUserHandler class: " + ex.getMessage());
            //String error = ex.getMessage();
            ErrorResponse errorResponse = new ErrorResponse(ex.getMessage(), 500);
            String errorResponseJsonString = new GsonBuilder().serializeNulls().create().toJson(errorResponse, ErrorResponse.class);
            return response
                    .withStatusCode(500)
                    //.withBody("{\"error\": \"" + error.replaceAll("\"", "\\\\\"") + "\"}");
                    .withBody(errorResponseJsonString);
        }

        return response;
    }
}
