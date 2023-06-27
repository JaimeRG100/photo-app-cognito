package com.appsdeveloperblog.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.appsdeveloperblog.aws.lambda.Utils;
import com.appsdeveloperblog.aws.lambda.service.CognitoUserService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

import java.util.HashMap;
import java.util.Map;

public class AddUserToGroupHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final CognitoUserService cognitoUserService;
    private final String userPoolId;

    public AddUserToGroupHandler() {
        cognitoUserService = new CognitoUserService(System.getenv("AWS_REGION"));
        userPoolId =  Utils.decryptKey("COGNITO_POOL_APP_ID");
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        LambdaLogger logger = context.getLogger();
        try {
            String requestBodyString = request.getBody();
            JsonObject jsonObject = JsonParser.parseString(requestBodyString).getAsJsonObject();
            String group = jsonObject.get("group").getAsString();
            //String userName = jsonObject.get("userName").getAsString(); // email or UUID
            String userName = request.getPathParameters().get("userName");
            JsonObject addUserToGroupResult = cognitoUserService.addUserToGroup(group, userName, userPoolId);
            response.withBody(new Gson().toJson(addUserToGroupResult, JsonObject.class));
            response.withStatusCode(200);
        } catch (AwsServiceException ex) {
            logger.log("AwsServiceException in AddUserToGroupHandler class: " + ex.awsErrorDetails().errorMessage());
            ErrorResponse errorResponse = new ErrorResponse(ex.awsErrorDetails().errorMessage(), ex.awsErrorDetails().sdkHttpResponse().statusCode());
            String errorResponseJsonString = new GsonBuilder().serializeNulls().create().toJson(errorResponse, ErrorResponse.class);
            return response
                    .withStatusCode(ex.awsErrorDetails().sdkHttpResponse().statusCode())
                    .withBody(errorResponseJsonString);
        } catch (RuntimeException ex) {
            logger.log("RuntimeException in AddUserToGroupHandler class: " + ex.getMessage());
            ErrorResponse errorResponse = new ErrorResponse(ex.getMessage(), 500);
            String errorResponseJsonString = new GsonBuilder().serializeNulls().create().toJson(errorResponse, ErrorResponse.class);
            return response
                    .withStatusCode(500)
                    .withBody(errorResponseJsonString);
        }
        return response;
    }
}
