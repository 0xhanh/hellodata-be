package ch.bedag.dap.hellodata.sidecars.omd.client;

import ch.bedag.dap.hellodata.sidecars.omd.client.user.response.OmdUserResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.log4j.Log4j2;
import org.openmetadata.client.api.*;
import org.openmetadata.client.gateway.OpenMetadata;
import org.openmetadata.client.model.*;
import org.openmetadata.schema.services.connections.metadata.OpenMetadataConnection;
import org.openmetadata.schema.security.client.OpenMetadataJWTClientConfig;
import org.openmetadata.schema.security.client.CustomOIDCSSOClientConfig;
import ch.bedag.dap.hellodata.sidecars.omd.config.properties.OmdProperties;
import org.openmetadata.schema.services.connections.metadata.AuthProvider;

import java.io.IOException;

@Log4j2
public class OmdGatewayClient {
    private OmdProperties omdProperties;
    private OpenMetadataConnection connection;
    private OpenMetadata gatewayClient;
    private boolean isConnected = false;

    public OmdGatewayClient(OmdProperties omdProperties) {
        this.omdProperties = omdProperties;
    }

    public void connectToOmd() {
        String providerType = omdProperties.getAuthProvider().getType();
        OmdProperties.ProviderConfig confProvider = omdProperties.getAuthProvider().getConfig();

        connection = new OpenMetadataConnection();
        connection.setHostPort(omdProperties.getApiEndpoint());
        connection.setApiVersion("v1");

        if (AuthProvider.CUSTOM_OIDC.toString().equals(providerType)) {
            log.info("Using custom OIDC config..");
            connection.setAuthProvider(AuthProvider.CUSTOM_OIDC);
            //CustomOIDCSSOClientConfig config = new CustomOIDCSSOClientConfig();
            //config.setClientId(kcClientId);
            //config.setSecretKey(kcClientSecret);
            //config.setTokenEndpoint(kcEndpoint);
            log.info("Unsupported auth provider type: " + providerType);
            throw new IllegalArgumentException("Unsupported auth provider type: " + providerType);
        } else if (AuthProvider.OPENMETADATA.toString().equals(providerType)) {
            log.info("Using custom jwt config");
            connection.setAuthProvider(AuthProvider.OPENMETADATA);
            OpenMetadataJWTClientConfig jwtConfig = new OpenMetadataJWTClientConfig();
            jwtConfig.setJwtToken(omdProperties.getAuthProvider().getConfig().getJwtToken());
            connection.setSecurityConfig(jwtConfig);
            // OpenMetadata Gateway
            this.gatewayClient = new OpenMetadata(connection);
            log.info("Connected to OpenMetadata Gateway");
            isConnected = true;
        } else {
            throw new IllegalArgumentException("Unsupported auth provider type: " + providerType);
        }
    }

    public OpenMetadata getGatewayClient() {
        if (!isConnected) {
            connectToOmd();
        }
        return this.gatewayClient;
    }

    public static ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    public UserList users() throws IOException {
        int limit = 500;
        UsersApi usersApi = getGatewayClient().buildClient(UsersApi.class);
        UserList users = usersApi.listUsers(new UsersApi.ListUsersQueryParams().limit(limit));
        return  users;
    }

    public boolean checkEmailInUse(String email) throws IOException {
        UsersApi usersApi = getGatewayClient().buildClient(UsersApi.class);
        return usersApi.checkEmailInUse((new EmailRequest().email(email)));
    }

    public OmdUserResponse toOmdUserResponse(User user) {
        OmdUserResponse omdUserResponse = new OmdUserResponse();
        omdUserResponse.setUsername(user.getName());
        omdUserResponse.setEmail(user.getEmail());
        omdUserResponse.setFirstName(user.getDisplayName());
        omdUserResponse.setLastName(user.getDisplayName());
        omdUserResponse.setActive(!Boolean.FALSE.equals(user.getDeleted()));
        return omdUserResponse;
    }

    public OmdUserResponse createUser(CreateUser createUser) throws IOException {
        UsersApi usersApi = getGatewayClient().buildClient(UsersApi.class);
        boolean isEmailInUse = usersApi.checkEmailInUse((new EmailRequest()).email(createUser.getEmail()));
        if (isEmailInUse) {
            log.error("Email already in use: {}", createUser.getEmail());
            throw new IllegalArgumentException("Email already in use: " + createUser.getEmail());
        }

        User user = usersApi.createUser(createUser);
        OmdUserResponse response = null;
        if (user != null) {
            log.debug("User created: {}", user.getDisplayName());
            response = new OmdUserResponse();
        }

        return response;
    }

    /**
     * Gets a user by username or email
     *
     * @param username the username to search for
     * @param email the email to search for
     * @return the user if found, null otherwise
     */
    public User getUser(String username, String email) throws IOException {
        UsersApi usersApi = getGatewayClient().buildClient(UsersApi.class);

        User foundItem = null;
        if (email != null && !email.isEmpty()) {
            // Use the listUsers endpoint with filtering
            UserList userList = usersApi.listUsers(new UsersApi.ListUsersQueryParams().limit(1000));
            if (userList != null && userList.getData() != null) {
                // Find the user with the matching email
                foundItem = userList.getData().stream()
                        .filter(u -> email.equalsIgnoreCase(u.getEmail()))
                        .findFirst()
                        .orElse(null);
            }
        }

        // Try to get by username if provided
        if (foundItem == null && username != null && !username.isEmpty()) {
            try {
                foundItem = usersApi.getUserByFQN(username, null, null);
            } catch (Exception e) {
                log.debug("User not found by username: {}", username);
            }
        }

        return foundItem;
    }

    /**
     * Delete a user with the following username.
     */
    public void deleteUser(String username, String email) {
        UsersApi usersApi = getGatewayClient().buildClient(UsersApi.class);
        User user = usersApi.getUserByFQN(username, new UsersApi.GetUserByFQNQueryParams().include("non-deleted"));
        if (user == null) {
            log.info("User {} doesn't exist in instance, omitting. Email: {}", username, email);
            return;
        }
        usersApi.deleteUser(user.getId(), new UsersApi.DeleteUserQueryParams().hardDelete(true));
        log.info("Deleted user {} doesn't exist in instance, omitting. Email: {}", username, email);
    }

    public void deleteSoftUser(User user) {
        if (user == null) {
            return;
        }
        UsersApi usersApi = getGatewayClient().buildClient(UsersApi.class);
        usersApi.deleteUser(user.getId(), new UsersApi.DeleteUserQueryParams().hardDelete(false));
    }

    /**
     * Update (or rather set) the roles of a user
     **/
    public CreateUser updateUser(CreateUser createUser) throws IOException {
        UsersApi usersApi = getGatewayClient().buildClient(UsersApi.class);

        return usersApi.createOrUpdateUser(createUser);
    }

    public Role createRole(CreateRole createRole) throws IOException {
        RolesApi roleApi = getGatewayClient().buildClient(RolesApi.class);
        return roleApi.createRole(createRole);
    }
    /**
     * Get List of roles available in Omd
     */
    public RoleList roles() throws IOException {
        RolesApi roleApi = getGatewayClient().buildClient(RolesApi.class);
        return roleApi.listRoles(new RolesApi.ListRolesQueryParams().limit(200));
    }

    public DomainList domains() throws IOException {
        DomainsApi domainApi = getGatewayClient().buildClient(DomainsApi.class);
        return domainApi.listDomains(new DomainsApi.ListDomainsQueryParams());
    }

    /**
     * Get List of policies available in OMD
     * @return
     * @throws IOException
     */
    public PolicyList policies() throws IOException {
        PoliciesApi policyApi = getGatewayClient().buildClient(PoliciesApi.class);
        return policyApi.listPolicies(new PoliciesApi.ListPoliciesQueryParams().limit(500));
    }
}
