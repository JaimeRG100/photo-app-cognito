package com.appsdeveloperblog.aws.lambda.service;

import com.appsdeveloperblog.aws.lambda.shared.Constants;
import com.google.gson.JsonObject;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.nio.charset.StandardCharsets;
import java.util.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


public class CognitoUserService {

    private final CognitoIdentityProviderClient cognitoIdentityProviderClient;

    public CognitoUserService(String region) {
        this.cognitoIdentityProviderClient = CognitoIdentityProviderClient
                .builder()
                .region(Region.of(region))
                .build();
    }

    public JsonObject createUser(JsonObject user, String appClientId, String appClientSecret) {

        String userEmail = user.get("email").getAsString();
        String userPassword = user.get("password").getAsString();
        //String userId = user.get("userId").getAsString(); // we can read from user or generate randomly
        String userId = UUID.randomUUID().toString();
        String userFirstName = user.get("firstName").getAsString();
        String userLastName = user.get("lastName").getAsString();


        AttributeType attributeEmail = AttributeType.builder()
                .name("email") // custom attribute that we added
                .value(userEmail)
                .build();

        AttributeType attributeName = AttributeType.builder()
                .name("email") // custom attribute that we added
                .value(userEmail)
                .build();

        AttributeType attributeUserId = AttributeType.builder()
                .name("custom:userId") // custom attribute that we added
                .value(userId)
                .build();

        List<AttributeType> attributes = new ArrayList<>();
        attributes.add(attributeEmail);
        attributes.add(attributeName);
        attributes.add(attributeUserId);

        String userName;
        String generatedSecretHash = calculateSecretHash(appClientId, appClientSecret, userEmail);

        SignUpRequest signUpRequest = SignUpRequest.builder()
                .username(userEmail)
                .password(userPassword)
                .userAttributes(attributes)
                .clientId(appClientId)
                .secretHash(generatedSecretHash) // Generate a client secret ENABLED
                .build();

        SignUpResponse signUpResponse = cognitoIdentityProviderClient.signUp(signUpRequest);
        JsonObject createUserResult = new JsonObject();
        createUserResult.addProperty(Constants.IS_SUCCESSFUL,signUpResponse.sdkHttpResponse().isSuccessful());
        createUserResult.addProperty(Constants.STATUS_CODE,signUpResponse.sdkHttpResponse().statusCode());
        createUserResult.addProperty(Constants.COGNITO_USER_ID,signUpResponse.userSub()); // userId
        createUserResult.addProperty(Constants.IS_CONFIRMED,signUpResponse.userConfirmed());
        return createUserResult;
    }

    public JsonObject confirmUserSignUp(String appClientId, String appClientSecret, String userEmail, String confirmationCode) {
        String generatedSecretHash = calculateSecretHash(appClientId, appClientSecret, userEmail);

        ConfirmSignUpRequest confirmSignUpRequest = ConfirmSignUpRequest.builder()
                .secretHash(generatedSecretHash)
                .username(userEmail)
                .confirmationCode(confirmationCode)
                .clientId(appClientId)
                .build();

        ConfirmSignUpResponse confirmSignUpResponse = cognitoIdentityProviderClient.confirmSignUp(confirmSignUpRequest);
        JsonObject confirmSignUpResult = new JsonObject();
        confirmSignUpResult.addProperty("isSuccessful", confirmSignUpResponse.sdkHttpResponse().isSuccessful());
        confirmSignUpResult.addProperty("statusCode", confirmSignUpResponse.sdkHttpResponse().statusCode());
        return confirmSignUpResult;
    }

    public JsonObject userLogin(JsonObject loginDetails, String appClientId, String appClientSecret) {
        String userEmail = loginDetails.get("email").getAsString();
        String userPassword = loginDetails.get("password").getAsString();
        String generatedSecretHash = calculateSecretHash(appClientId, appClientSecret, userEmail);

        Map<String, String> authParams = new HashMap<String, String>() {
            {
                put("USERNAME", userEmail);
                put("PASSWORD", userPassword);
                put("SECRET_HASH", generatedSecretHash);
            }
        };

        InitiateAuthRequest initiateAuthRequest = InitiateAuthRequest.builder()
                .clientId(appClientId)
                .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                .authParameters(authParams)
                .build();

        InitiateAuthResponse initiateAuthResponse = cognitoIdentityProviderClient.initiateAuth(initiateAuthRequest);
        AuthenticationResultType authenticationResultType = initiateAuthResponse.authenticationResult();

        JsonObject loginResult = new JsonObject();
        loginResult.addProperty("isSuccessful", initiateAuthResponse.sdkHttpResponse().isSuccessful());
        loginResult.addProperty("statusCode", initiateAuthResponse.sdkHttpResponse().statusCode());

        loginResult.addProperty("idToken", authenticationResultType.idToken());
        //  JWT token that contains claims about the identity of the authenticated user, such as name, email and phone number.
        //  You can use this token to authenticate users to your resource service or server applications

        loginResult.addProperty("accessToken", authenticationResultType.accessToken());
        // It also contains claims about the authenticated user and also a list of user groups and a list of scopes.
        //And the purpose of access token is to authorize API operations in the context of a user in the user pool.
        //For example, you can use this access token to grant user access to add, change or delete user attributes.

        loginResult.addProperty("refreshToken", authenticationResultType.refreshToken());
        // you will use to retrieve new I.D. token and new access tokens once this to expire.

        return loginResult;
    }


    public JsonObject addUserToGroup(String groupName, String username, String userPoolId) {

        AdminAddUserToGroupRequest adminAddUserToGroupRequest = AdminAddUserToGroupRequest.builder()
                .groupName(groupName)
                .username(username)
                .userPoolId(userPoolId)
                .build();

        AdminAddUserToGroupResponse adminAddUserToGroupResponse = cognitoIdentityProviderClient.adminAddUserToGroup(adminAddUserToGroupRequest);

        JsonObject adminAddUserToGroupResult = new JsonObject();
        adminAddUserToGroupResult.addProperty("isSuccessful", adminAddUserToGroupResponse.sdkHttpResponse().isSuccessful());
        adminAddUserToGroupResult.addProperty("statusCode", adminAddUserToGroupResponse.sdkHttpResponse().statusCode());
        return adminAddUserToGroupResult;
    }



    public JsonObject getUser(String accessToken) {
        GetUserRequest getUserRequest = GetUserRequest.builder()
                .accessToken(accessToken)
                .build();
        GetUserResponse userResponse = cognitoIdentityProviderClient.getUser(getUserRequest);
        JsonObject userResult = new JsonObject();
        userResult.addProperty("isSuccessful", userResponse.sdkHttpResponse().isSuccessful());
        userResult.addProperty("statusCode", userResponse.sdkHttpResponse().statusCode());

        List<AttributeType> userAttributes = userResponse.userAttributes();
        JsonObject userDetails = new JsonObject();
        userAttributes.forEach(attribute -> {
            userDetails.addProperty(attribute.name(), attribute.value());
        });

        userResult.add("user", userDetails);
        return userResult;
    }



    public String calculateSecretHash(String userPoolClientId, String userPoolClientSecret, String userName) {
        final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

        SecretKeySpec signingKey = new SecretKeySpec(
                userPoolClientSecret.getBytes(StandardCharsets.UTF_8),
                HMAC_SHA256_ALGORITHM);
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            mac.init(signingKey);
            mac.update(userName.getBytes(StandardCharsets.UTF_8));
            byte[] rawHmac = mac.doFinal(userPoolClientId.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Error while calculating ");
        }
    }
}
