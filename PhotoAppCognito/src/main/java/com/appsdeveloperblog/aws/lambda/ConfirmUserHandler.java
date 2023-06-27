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

public class ConfirmUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final CognitoUserService cognitoUserService;
    private final String appClientId;
    private final String appClientSecret;

    public ConfirmUserHandler(){
        this.cognitoUserService= new CognitoUserService(System.getenv("AWS_REGION"));
        //appClientId = System.getenv("COGNITO_POOL_APP_CLIENT_ID");
        appClientId = Utils.decryptKey("COGNITO_POOL_APP_CLIENT_ID");
        //appClientSecret = System.getenv("COGNITO_POOL_APP_CLIENT_SECRET");
        appClientSecret = Utils.decryptKey("COGNITO_POOL_APP_CLIENT_SECRET");
    }

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent request, final Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        LambdaLogger logger = context.getLogger();
        try {
            String requestBodyString = request.getBody();
            JsonObject requestBody = JsonParser.parseString(requestBodyString).getAsJsonObject();
            String email = requestBody.get("email").getAsString();
            String confirmationCode = requestBody.get("code").getAsString();

            JsonObject confirmUserResult = cognitoUserService.confirmUserSignUp(appClientId, appClientSecret, email, confirmationCode);
            response.withStatusCode(200);
            response.withBody(new Gson().toJson(confirmUserResult, JsonObject.class));
        } catch (AwsServiceException ex) {
            logger.log("AwsServiceException in ConfirmUserHandler class: " + ex.awsErrorDetails().errorMessage());
            //String error = ex.awsErrorDetails().errorMessage();
            ErrorResponse errorResponse = new ErrorResponse(ex.awsErrorDetails().errorMessage(), ex.awsErrorDetails().sdkHttpResponse().statusCode());
            String errorResponseJsonString = new GsonBuilder().serializeNulls().create().toJson(errorResponse, ErrorResponse.class);
            return response
                    .withStatusCode(ex.awsErrorDetails().sdkHttpResponse().statusCode())
                    //.withBody("{\"error\": \"" + error.replaceAll("\"", "\\\\\"") + "\"}");
                    .withBody(errorResponseJsonString);
        } catch (RuntimeException ex) {
            logger.log("RuntimeException in ConfirmUserHandler class: " + ex.getMessage());
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