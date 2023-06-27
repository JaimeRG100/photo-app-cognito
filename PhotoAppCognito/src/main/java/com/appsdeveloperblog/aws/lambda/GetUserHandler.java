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

public class GetUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final CognitoUserService cognitoUserService;

    public GetUserHandler() {
        cognitoUserService = new CognitoUserService(System.getenv("AWS_REGION"));
    }


    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent request, final Context context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(headers);

        Map<String, String> requestHeaders = request.getHeaders();
        LambdaLogger logger = context.getLogger();

        try {
            JsonObject userDetails = cognitoUserService.getUser(requestHeaders.get("AccessToken"));
            response.withStatusCode(200);
            //response.withBody("{\"status\":\"Working...\"}");
            response.withBody(new Gson().toJson(userDetails, JsonObject.class));
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
