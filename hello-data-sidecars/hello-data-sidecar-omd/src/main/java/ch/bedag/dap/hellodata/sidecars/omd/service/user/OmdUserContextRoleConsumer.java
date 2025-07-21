package ch.bedag.dap.hellodata.sidecars.omd.service.user;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.UserContextRoleUpdate;
import ch.bedag.dap.hellodata.sidecars.omd.client.OmdGatewayClient;
import ch.bedag.dap.hellodata.sidecars.omd.service.provider.OmdGatewayClientProvider;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.openmetadata.client.model.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.UPDATE_USER_CONTEXT_ROLE;

@Log4j2
@Service
@AllArgsConstructor
public class OmdUserContextRoleConsumer {

    private final OmdGatewayClientProvider omdClientProvider;
    private static final String NONE_ROLE_NAME = "NONE";

    /**
     * Handles user context role updates from the event stream
     * Example payload:
     * {"email":"hanhvd@bedag.ch","username":"hanhvd@bedag.ch","active":true,
     * "contextRoles":[{"contextKey":"Default_Data_Domain","parentContextKey":null,"roleName":"DATA_DOMAIN_ADMIN"},
     * {"contextKey":"Extra_Data_Domain","parentContextKey":null,"roleName":"DATA_DOMAIN_ADMIN"},
     * {"contextKey":"HelloDATA_Product_Development","parentContextKey":null,"roleName":"BUSINESS_DOMAIN_ADMIN"}],
     * "extraModuleRoles":{},"sendBackUsersList":true,"first_name":"Vũ","last_name":"Hanh"}
     */
    /**
     * UserContextRoleUpdate(email=test2@bedag.ch, username=test2@bedag.ch, firstName=Test, lastName=Test, active=true,
     * contextRoles=[UserContextRoleUpdate.ContextRole(contextKey=Default_Data_Domain, parentContextKey=null, roleName=DATA_DOMAIN_EDITOR),
     * UserContextRoleUpdate.ContextRole(contextKey=Extra_Data_Domain, parentContextKey=null, roleName=DATA_DOMAIN_ADMIN),
     * UserContextRoleUpdate.ContextRole(contextKey=HelloDATA_Product_Development, parentContextKey=null, roleName=NONE)],
     * extraModuleRoles={}, sendBackUsersList=true)
     * @param userContextRoleUpdate
     * @throws URISyntaxException
     * @throws IOException
     */
    @SuppressWarnings("unused")
    @JetStreamSubscribe(event = UPDATE_USER_CONTEXT_ROLE)
    public void subscribe(UserContextRoleUpdate userContextRoleUpdate) throws URISyntaxException, IOException {
        log.info("Update user context roles {}", userContextRoleUpdate);
        updateUserContextRoles(userContextRoleUpdate);
    }

    public void updateUserContextRoles(UserContextRoleUpdate userContextRoleUpdate) throws URISyntaxException, IOException {
        OmdGatewayClient omdClient = omdClientProvider.getOmdGatewayClientInstance();
        // Get user and validate
        User user = getUserAndValidate(omdClient, userContextRoleUpdate);
        if (user == null) {
            return;
        }

        log.debug("Found User {}", user);

        // Handle user deactivation
        if (!userContextRoleUpdate.getActive()) {
            if (handleUserDeactivation(omdClient, user, userContextRoleUpdate)) {
                return;
            }
        }

        // Update user with domain and role assignments
        updateUserContextRoles(omdClient, user, userContextRoleUpdate);
    }

    /**
     * Retrieves and validates the user
     */
    private User getUserAndValidate(OmdGatewayClient omdClient, UserContextRoleUpdate userContextRoleUpdate)
            throws URISyntaxException, IOException {
        if (OmdUserUtil.isAdmin(userContextRoleUpdate.getUsername())){
            log.warn("User is an admin user {}. Ignore update for the admin user", userContextRoleUpdate);
            return null;
        }

        User user = omdClient.getUser(userContextRoleUpdate.getUsername(), userContextRoleUpdate.getEmail());
        if (user == null) {
            log.warn("User not found {}", userContextRoleUpdate);
            return null;
        }
        if (Boolean.TRUE.equals(user.getIsBot())) {
            log.warn("User is a bot user {}. Ignore update for the bot user", userContextRoleUpdate);
            return null;
        }

        return user;
    }

    /**
     * Handles user deactivation logic
     * @return true if the process should exit after deactivation
     */
    private boolean handleUserDeactivation(OmdGatewayClient omdClient, User user, UserContextRoleUpdate userContextRoleUpdate)
            throws URISyntaxException, IOException {
        // Don't deactivate admin user
        if (user.getName().equalsIgnoreCase("admin")) {
            log.warn("User is admin {}. Ignore set disable for admin user", userContextRoleUpdate);
            return true;
        }

        // If user is already inactive, just soft delete
        boolean isDeleted = Boolean.TRUE.equals(user.getDeleted());
        if (!isDeleted) {
            omdClient.deleteSoftUser(user);
            log.info("User {} is deleted. Ignore update others", userContextRoleUpdate.getUsername());
            return true;
        }

        return false;
    }

    /**
     * Updates user with domain and role assignments
     */
    private void updateUserContextRoles(OmdGatewayClient omdClient, User user, UserContextRoleUpdate userContextRoleUpdate){
        try {
            // Match domains and roles
            List<EntityReference> matchingDomainRefs = getMatchingDomains(omdClient, userContextRoleUpdate);
            List<EntityReference> matchingRoleRefs = getMatchingRoles(omdClient, userContextRoleUpdate);

            // Only proceed if we have valid domains or roles to assign
            if ((matchingDomainRefs == null || matchingDomainRefs.isEmpty()) &&
                    (matchingRoleRefs == null || matchingRoleRefs.isEmpty())) {
                log.warn("No valid domains or roles found for user {}. Skipping update.", userContextRoleUpdate.getUsername());
            } else {
                // Update user with matched domains and roles
                user.setDomains(matchingDomainRefs);
                user.setRoles(matchingRoleRefs);
                log.info("Matching domain references: {}", matchingDomainRefs);
                log.info("Matching roles references: {}", matchingRoleRefs);
            }

            // Convert and update the user
            CreateUser createUser = EntityReferenceConverter.convertUserToCreateUser(user);
            omdClient.updateUser(createUser);
            log.info("Successfully updated user {} with {} domains and {} roles",
                    user.getName(),
                    matchingDomainRefs != null ? matchingDomainRefs.size() : 0,
                    matchingRoleRefs != null ? matchingRoleRefs.size() : 0);
        }catch (Exception e) {
            log.error("Error while updating user '{}'",
                    userContextRoleUpdate.getUsername(), e);
        }
    }

    /**
     * Finds matching domains based on context keys
     */
    private List<EntityReference> getMatchingDomains(OmdGatewayClient omdClient, UserContextRoleUpdate userContextRoleUpdate)
            throws URISyntaxException, IOException {
        List<UserContextRoleUpdate.ContextRole> contextRoles = userContextRoleUpdate.getContextRoles();
        if (contextRoles.isEmpty()) {
            log.warn("No context roles found. Skip domain matching for {}", userContextRoleUpdate);
            return new ArrayList<>();
        }

        DomainList domains = omdClient.domains();
        if (domains == null || CollectionUtils.isEmpty(domains.getData())) {
            log.error("No domains found in OMD. Skip domain matching for {}", userContextRoleUpdate);
            return new ArrayList<>();
        }

    return domains.getData().stream()
            .filter(domain -> contextRoles.stream()
                    .anyMatch(contextRole -> domain.getFullyQualifiedName().equalsIgnoreCase(contextRole.getContextKey())
                        && !contextRole.getRoleName().name().equalsIgnoreCase(NONE_ROLE_NAME)))
            .map(EntityReferenceConverter::convertDomainToEntityReference)
            .collect(Collectors.toList());
}

    /**
     * Finds matching roles based on role names, creating them if they don't exist
     */
    private List<EntityReference> getMatchingRoles(OmdGatewayClient omdClient, UserContextRoleUpdate userContextRoleUpdate)
            throws URISyntaxException, IOException {
        List<UserContextRoleUpdate.ContextRole> contextRoles = userContextRoleUpdate.getContextRoles();
        if (contextRoles.isEmpty()) {
            log.warn("No context roles found. Skip role matching for {}", userContextRoleUpdate);
            return new ArrayList<>();
        }

        // Get existing roles from OMD
        List<Role> existingRoles = omdClient.roles().getData();

        // 1. Lọc ra các role names từ contextRoles
        final Set<String> requestedRoleNames = contextRoles.stream()
                .map(contextRole -> contextRole.getRoleName().name())
                .collect(Collectors.toSet());

        // 2. Lọc ra các role names đã tồn tại trong OMD
        Set<String> existingRoleNames = existingRoles.stream()
                .map(Role::getName)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        // 3. Tìm các role names chưa tồn tại trong OMD
        Set<String> rolesToCreate = requestedRoleNames.stream()
                .filter(roleName -> !existingRoleNames.contains(roleName.toUpperCase()))
                .collect(Collectors.toSet());

        // 4. Tạo các roles chưa tồn tại
        if (!rolesToCreate.isEmpty()) {
            log.info("Creating {} new roles in OMD: {}", rolesToCreate.size(), rolesToCreate);
            for (String roleName : rolesToCreate) {
                try {
                    createRoleInOmd(omdClient, roleName);
                    log.info("Successfully created role: {}", roleName);
                } catch (Exception e) {
                    log.error("Failed to create role {} in OMD: {}", roleName, e.getMessage());
                }
            }

            // 5. Lấy lại danh sách roles sau khi đã tạo mới
            RoleList roles = omdClient.roles();
            if (roles == null || roles.getData() == null) {
                log.error("Failed to retrieve updated roles from OMD after creation");
                // Sử dụng danh sách roles cũ nếu không lấy được danh sách mới
            } else {
                existingRoles = roles.getData();
            }
        }

        // 6. Matching và trả về danh sách EntityReference
        final Set<String> upperRequestedRoleNames = requestedRoleNames.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
        return existingRoles.stream()
                .filter(role -> upperRequestedRoleNames.contains(role.getName().toUpperCase()))
                .map(EntityReferenceConverter::convertRoleToEntityReference)
                .collect(Collectors.toList());
    }

    /**
     * Creates a new role in OMD
     */
    private Role createRoleInOmd(OmdGatewayClient omdClient, String roleName)
            throws IOException {
        // Create a new role with default policy
        CreateRole createRole = new CreateRole();
        createRole.setName(roleName);
        createRole.setDisplayName(formatDisplayName(roleName));
        createRole.setDescription("Auto-generated role from HelloData");
        createRole.setPolicies(getDefaultPolicies(omdClient));

        // Call OMD API to create the role
        return omdClient.createRole(createRole);
    }

    /**
     * Formats the role name for display (e.g., DATA_DOMAIN_ADMIN -> Data Domain Admin)
     */
    private String formatDisplayName(String roleName) {
        String[] words = roleName.split("_");
        StringBuilder displayName = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                displayName.append(word.substring(0, 1).toUpperCase())
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }

        return displayName.toString().trim();
    }

    /**
     * Returns default policies for auto-generated roles
     */
    private List<String> getDefaultPolicies(OmdGatewayClient omdClient) throws IOException {
        // In a real implementation, you would retrieve appropriate default policies
        // For now, return an empty list or reference to a default policy if available
//        String defaultPolicy = omdClient.policies().getData()
//                .stream().filter(p -> p.getName().equalsIgnoreCase("DataConsumerPolicy"))
//                .map(Policy::getName)
//                .findFirst().orElse(null));

        return Collections.singletonList("DataConsumerPolicy");
    }
}
