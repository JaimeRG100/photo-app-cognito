/*
package com.appsdeveloperblog.aws.lambda;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.appsdeveloperblog.aws.lambda.service.CognitoUserService;
import com.appsdeveloperblog.aws.lambda.shared.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.SdkHttpResponse;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class CreateUserHandlerTest {

  @Mock
  private CognitoUserService cognitoUserService;

  @Mock
  APIGatewayProxyRequestEvent request;

  @Mock
  Context context;

  @Mock
  LambdaLogger lambdaLoggerMock;

  @InjectMocks
  private CreateUserHandler createUserHandler;

  @BeforeEach
  public void runBeforeEachTestMethod() {
    System.out.println("Executing @BeforeEach method");
    when(context.getLogger()).thenReturn(lambdaLoggerMock);

  }

  @AfterEach
  public void runAfterEachTestMethod() {
    System.out.println("Executing @AfterEach method");

  }

  @BeforeAll
  public static void runBeforeAllTestMethod() {
    System.out.println("Executing @BeforeAll method");

  }

  @AfterAll
  public static void runAfterAllTestMethod() {
    System.out.println("Executing @AfterAll method");

  }

  @Test
  public void testHandleRequest_whenValidDetailsProvided_returnsSuccessfulResponse() {

    // Arrange    or Given
    JsonObject userDetails = new JsonObject();
    userDetails.addProperty("firstName", "James");
    userDetails.addProperty("lastName", "Douglas");
    userDetails.addProperty("email", "some@email.com");
    userDetails.addProperty("password", "123456789");
    String userDetailsString = new Gson().toJson(userDetails, JsonObject.class);
    when(request.getBody()).thenReturn(userDetailsString);

    when(context.getLogger()).thenReturn(lambdaLoggerMock);

    JsonObject createUserResult = new JsonObject();
    createUserResult.addProperty(Constants.IS_SUCCESSFUL, true);
    createUserResult.addProperty(Constants.STATUS_CODE, 200);
    createUserResult.addProperty(Constants.COGNITO_USER_ID, UUID.randomUUID().toString());
    createUserResult.addProperty("isConfirmed", false);
    when(cognitoUserService.createUser(any(), any(), any())).thenReturn(createUserResult);

    // Act        or When
    APIGatewayProxyResponseEvent response = createUserHandler.handleRequest(request, context);
    String responseBody = response.getBody();
    JsonObject responseBodyJson = JsonParser.parseString(responseBody).getAsJsonObject();


    // Assert     or Then
    verify(lambdaLoggerMock, times(1)).log(anyString());
    verify(cognitoUserService, times(1)).createUser(any(), any(), any());
    assertNotNull(response);

    assertTrue(responseBodyJson.get(Constants.IS_SUCCESSFUL).getAsBoolean());
    assertEquals(200, responseBodyJson.get(Constants.STATUS_CODE).getAsInt(), "Successful HTTP Response should have HTTP status code 200");
    assertNotNull(responseBodyJson.get(Constants.COGNITO_USER_ID).getAsString());
    assertFalse(responseBodyJson.get(Constants.IS_CONFIRMED).getAsBoolean());
  }



  @Test
  public void testHandleRequest_whenInvalidDetailsProvided_returnsErrorMessage() {

    // Arrange    or Given
    when(request.getBody()).thenReturn("");

    // Act        or When
    APIGatewayProxyResponseEvent response = createUserHandler.handleRequest(request, context);
    String responseBody = response.getBody();
    JsonObject responseBodyJson = JsonParser.parseString(responseBody).getAsJsonObject();

    // Assert     or Then
    assertEquals(500, response.getStatusCode());
    assertNotNull(responseBodyJson.get("message"), "Missing the 'message' property in JSON response.");
    assertFalse(responseBodyJson.get("message").getAsString().isEmpty(), "Error message should not be empty");

  }



  @Test
  public void testHandleRequest_whenAwsServiceExceptionTakesPlace_returnsErrorMessage() {
    // Arrange    or Given
    when(request.getBody()).thenReturn("{}");
    AwsErrorDetails awsErrorDetails = AwsErrorDetails.builder()
        .errorCode("")
        .sdkHttpResponse(SdkHttpResponse.builder().statusCode(500).build())
        .errorMessage("AwsServiceException took place")
        .build();
    when(cognitoUserService.createUser(any(), any(), any())).thenThrow(
        AwsServiceException.builder()
            .statusCode(500)
            .awsErrorDetails(awsErrorDetails)
            .build()
    );

    // Act        or When
    APIGatewayProxyResponseEvent response = createUserHandler.handleRequest(request, context);
    String responseBody = response.getBody();
    JsonObject responseBodyJson = JsonParser.parseString(responseBody).getAsJsonObject();

    // Assert     or Then
    assertEquals(awsErrorDetails.sdkHttpResponse().statusCode(), response.getStatusCode());
    assertNotNull(awsErrorDetails.errorMessage());
    assertEquals(awsErrorDetails.errorMessage(), responseBodyJson.get("message").getAsString());

    assertEquals(500, response.getStatusCode());
    assertNotNull(responseBodyJson.get("message"), "Missing the 'message' property in JSON response.");
    assertFalse(responseBodyJson.get("message").getAsString().isEmpty(), "Error message should not be empty");
  }

}
*/