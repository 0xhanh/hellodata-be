package ch.bedag.dap.hellodata.sidecars.omd.client;

import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.UserContextRoleUpdate;
import ch.bedag.dap.hellodata.sidecars.omd.client.user.response.OmdUserResponse;
import ch.bedag.dap.hellodata.sidecars.omd.service.provider.OmdGatewayClientProvider;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openmetadata.client.model.UserList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import ch.bedag.dap.hellodata.sidecars.omd.service.user.OmdUserContextRoleConsumer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openmetadata.client.model.CreateUser;
import org.openmetadata.client.model.EntityReference;
import org.openmetadata.client.model.Profile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Disabled("Only working for local development")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Log4j2
@ComponentScan("ch.bedag.dap.hellodata")
//@EnabledIf(expression = "${tests.spring.enabled:false}")
class OmdClientTest {

    @Autowired
    private OmdGatewayClientProvider omdGatewayClientProvider;
    @Autowired OmdUserContextRoleConsumer omdUserContextRoleConsumer;

    @Test
    void users() throws URISyntaxException, IOException {
        //When
        OmdGatewayClient gatewayClient = omdGatewayClientProvider.getOmdGatewayClientInstance();
        UserList users = gatewayClient.users();
        for (org.openmetadata.client.model.User user : users.getData()) {
            log.info(user.toString());
        }
        //Then
        assertNotNull(users);
        assert !users.getData().isEmpty();
    }

    @Test
    void createUser() throws URISyntaxException, IOException {
        //Given
        org.openmetadata.client.model.CreateUser user = new org.openmetadata.client.model.CreateUser();
        user.setEmail("test@bedag.ch");
        user.displayName("test");
        user.setIsBot(false);
        user.setName("test");
        user.description("user test");
        user.setRoles(new ArrayList<>());

        //When
        OmdGatewayClient gatewayClient = omdGatewayClientProvider.getOmdGatewayClientInstance();
        OmdUserResponse createdUser = gatewayClient.createUser(user);
        //Then
        assertNotNull(createdUser);
    }

    /**
     * Handles user context role updates from the event stream
     * Example payload:
     * {"email":"hanhvd@bedag.ch","username":"hanhvd@bedag.ch","active":true,
     * "contextRoles":[{"contextKey":"Default_Data_Domain","parentContextKey":null,"roleName":"DATA_DOMAIN_ADMIN"},
     * {"contextKey":"Extra_Data_Domain","parentContextKey":null,"roleName":"DATA_DOMAIN_ADMIN"},
     * {"contextKey":"HelloDATA_Product_Development","parentContextKey":null,"roleName":"BUSINESS_DOMAIN_ADMIN"}],
     * "extraModuleRoles":{},"sendBackUsersList":true,"first_name":"Vũ","last_name":"Hanh"}
     * DATA_Product_Development.Default_Data_Domain
     */
    @Test
    void updateUser() throws IOException, URISyntaxException {
        //Given
        UserContextRoleUpdate userContextRoleUpdate = new UserContextRoleUpdate();
        userContextRoleUpdate.setEmail("hehe@bedag.ch");
        userContextRoleUpdate.setUsername("hehe");
        userContextRoleUpdate.setActive(true);

        List<UserContextRoleUpdate.ContextRole> contextRoles = new ArrayList<>();

        UserContextRoleUpdate.ContextRole contextRole1 = new UserContextRoleUpdate.ContextRole();
        contextRole1.setContextKey("Default_Data_Domain");
        contextRole1.setParentContextKey(null);
        contextRole1.setRoleName(HdRoleName.DATA_DOMAIN_ADMIN);
        contextRoles.add(contextRole1);

        UserContextRoleUpdate.ContextRole contextRole2 = new UserContextRoleUpdate.ContextRole();
        contextRole2.setContextKey("Extra_Data_Domain");
        contextRole2.setParentContextKey(null);
        contextRole2.setRoleName(HdRoleName.DATA_DOMAIN_ADMIN);
        contextRoles.add(contextRole2);

//        UserContextRoleUpdate.ContextRole contextRole3 = new UserContextRoleUpdate.ContextRole();
//        contextRole3.setContextKey("HelloDATA_Product_Development");
//        contextRole3.setParentContextKey(null);
//        contextRole3.setRoleName(HdRoleName.BUSINESS_DOMAIN_ADMIN);
//        contextRoles.add(contextRole3);

        userContextRoleUpdate.setContextRoles(contextRoles);
        userContextRoleUpdate.setExtraModuleRoles(new HashMap<>());
        userContextRoleUpdate.setSendBackUsersList(true);
        userContextRoleUpdate.setFirstName("Vũ");
        userContextRoleUpdate.setLastName("Hanh");

        log.info("userContextRoleUpdate: {}", userContextRoleUpdate);
        //When
        try {
            omdUserContextRoleConsumer.updateUserContextRoles(userContextRoleUpdate);
        }catch (Exception e){
            e.printStackTrace();
            log.error(e.getMessage());
        }
        log.info("Done");
        //Then
    }


    @Test
    void testDeSerialize() throws URISyntaxException, IOException {
        try {
            // Create a new CreateUser object
            CreateUser createUser = new CreateUser();

            // Set required fields
            createUser.setName("user1");
            createUser.setEmail("user1@example.com");

            // Set optional fields
            createUser.setDisplayName("User One");
            createUser.setDescription("This is a test user");
            createUser.setIsAdmin(false);
            createUser.setIsBot(false);

            // Set profile information if needed
            Profile profile = new Profile();
            createUser.setProfile(profile);

            // Set team associations correctly
            // Note: Teams should be a list of UUIDs, not objects
            List<UUID> teamIds = new ArrayList<>();
            teamIds.add(UUID.fromString("dadfa1c8-51f4-4fd1-a01b-ae81919287aa"));
            createUser.setTeams(teamIds);

            // If you need to set domains
            List<String> domains = new ArrayList<>();
            domains.add("Default_Data_Domain");
            domains.add("Extra_Data_Domain");
            createUser.setDomains(domains);

            // Serialize to JSON to see what's being sent to the API
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(createUser);
            System.out.println("Serialized CreateUser object:");
            System.out.println(json);

            // Deserialize from JSON to verify it works correctly
            CreateUser deserializedUser = objectMapper.readValue(json, CreateUser.class);
            System.out.println("Deserialized CreateUser object:");
            System.out.println(deserializedUser);

            // Verify the teams field specifically
            System.out.println("Teams after deserialization:");
            System.out.println(deserializedUser.getTeams());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    @Test
//    void roles() throws URISyntaxException, IOException {
//        //When
//        AirflowRolesResponse roles = airflowClientProvider.getAirflowClientInstance().roles();
//
//        //Then
//        assertThat(roles).isNotNull();
//        assertThat(roles.getTotalEntries()).isGreaterThan(0);
//        assertThat(roles.getRoles()).isNotEmpty();
//    }
//
//    @Test
//    void permissions() throws URISyntaxException, IOException {
//        //When
//        AirflowPermissionsResponse permissions = airflowClientProvider.getAirflowClientInstance().permissions();
//
//        //Then
//        assertThat(permissions).isNotNull();
//        assertThat(permissions.getTotalEntries()).isGreaterThan(0);
//        assertThat(permissions.getActions()).isNotEmpty();
//    }
//
//    @NotNull
//    private static OmdUserRolesUpdate updateUserContextRoles(String usernameToUpdate) {
//        AirflowUserRolesUpdate userRolesUpdate = new AirflowUserRolesUpdate();
//        userRolesUpdate.setUsername(usernameToUpdate);
//        userRolesUpdate.setEmail("test");
//        userRolesUpdate.setFirstName("test");
//        userRolesUpdate.setLastName("last");
//        userRolesUpdate.setPassword("test");
//        ArrayList<AirflowUserRole> roles = new ArrayList<>();
//        AirflowUserRole role1 = new AirflowUserRole();
//        role1.setName("Viewer");
//        roles.add(role1);
//        AirflowUserRole role2 = new AirflowUserRole();
//        role2.setName("Admin");
//        roles.add(role2);
//        userRolesUpdate.setRoles(roles);
//        return userRolesUpdate;
//    }
}