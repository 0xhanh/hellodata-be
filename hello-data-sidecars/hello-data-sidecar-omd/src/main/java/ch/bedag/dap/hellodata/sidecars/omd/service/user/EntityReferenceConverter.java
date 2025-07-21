package ch.bedag.dap.hellodata.sidecars.omd.service.user;

import org.openmetadata.client.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Utility class for converting between entity types and references
 */
public class EntityReferenceConverter {

    /**
     * Converts a Domain object into an EntityReference
     *
     * @param domain The Domain object to convert
     * @return An EntityReference representing the Domain
     */
    public static EntityReference convertDomainToEntityReference(Domain domain) {
        if (domain == null) {
            return null;
        }

        EntityReference entityReference = new EntityReference();

        // Set required fields
        entityReference.setId(domain.getId());
        entityReference.setType("domain");  // The type for domains

        // Set optional fields if available
        entityReference.setName(domain.getName());
        entityReference.setDisplayName(domain.getDisplayName());
        entityReference.setDescription(domain.getDescription());
        entityReference.setFullyQualifiedName(domain.getFullyQualifiedName());
        entityReference.setDeleted(domain.getDeleted());
        entityReference.setHref(domain.getHref());

        return entityReference;
    }

    /**
     * Converts a list of Domains to a list of EntityReferences
     *
     * @param domains List of Domain objects
     * @return List of EntityReference objects
     */
    public static List<EntityReference> convertDomainsToEntityReferences(List<Domain> domains) {
        if (domains == null) {
            return null;
        }

        return domains.stream()
                .map(EntityReferenceConverter::convertDomainToEntityReference)
                .collect(Collectors.toList());
    }

    /**
     * Converts a Role object into an EntityReference
     *
     * @param role The Role object to convert
     * @return An EntityReference representing the Role
     */
    public static EntityReference convertRoleToEntityReference(Role role) {
        if (role == null) {
            return null;
        }

        EntityReference entityReference = new EntityReference();

        // Set required fields
        entityReference.setId(role.getId());
        entityReference.setType("role");  // The type for roles

        // Set optional fields if available
        entityReference.setName(role.getName());
        entityReference.setDisplayName(role.getDisplayName());
        entityReference.setDescription(role.getDescription());
        entityReference.setFullyQualifiedName(role.getFullyQualifiedName());
        entityReference.setDeleted(role.getDeleted());
        entityReference.setHref(role.getHref());

        return entityReference;
    }

    /**
     * Converts a list of Roles to a list of EntityReferences
     *
     * @param roles List of Role objects
     * @return List of EntityReference objects
     */
    public static List<EntityReference> convertRolesToEntityReferences(List<Role> roles) {
        if (roles == null) {
            return null;
        }

        return roles.stream()
                .map(EntityReferenceConverter::convertRoleToEntityReference)
                .collect(Collectors.toList());
    }

    public static CreateUser convertUserToCreateUser(User user) {
        if (user == null) {
            return null;
        }

        CreateUser createUser = new CreateUser();

        // Set required fields
        createUser.setName(user.getName());
        createUser.setEmail(user.getEmail());

        // Set optional fields
        createUser.setAuthenticationMechanism(user.getAuthenticationMechanism());
        createUser.setBotName(user.getIsBot() != null && user.getIsBot() ? user.getName() : null);
        createUser.setDescription(user.getDescription());
        createUser.setDisplayName(user.getDisplayName());

        // Handle domain
        if (user.getDomain() != null) {
            createUser.setDomain(user.getDomain().getName());
        }

        // Handle domains
        if (user.getDomains() != null && !user.getDomains().isEmpty()) {
            List<String> domainNames = user.getDomains().stream()
                    .map(EntityReference::getName)
                    .collect(Collectors.toList());
            createUser.setDomains(domainNames);
        }

        createUser.setExtension(user.getExtension());
        createUser.setIsAdmin(user.getIsAdmin());
        createUser.setIsBot(user.getIsBot());
        createUser.setLifeCycle(user.getLifeCycle());

        // Handle roles
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            List<UUID> roleIds = user.getRoles().stream()
                    .map(EntityReference::getId)
                    .collect(Collectors.toList());
            createUser.setRoles(roleIds);
        }

        // Handle teams
        if (user.getTeams() != null && !user.getTeams().isEmpty()) {
            List<UUID> teamIds = user.getTeams().stream()
                    .map(EntityReference::getId)
                    .collect(Collectors.toList());
            createUser.setTeams(teamIds);
        }
//        else{
//            UUID teamId = UUID.fromString("dadfa1c8-51f4-4fd1-a01b-ae81919287aa");
//            List<UUID> teams = Collections.singletonList(teamId);
//            createUser.setTeams(teams);
//            createUser.addTeamsItem(teamId);
//        }

        // Create a list of UUIDs
//        List<UUID> teamIds = new ArrayList<>();
//        teamIds.add(UUID.fromString("dadfa1c8-51f4-4fd1-a01b-ae81919287aa"));
//        createUser.setTeams(teamIds);

        // Set isAdmin flag
        createUser.setIsAdmin(false);
        createUser.setIsBot(false);

        createUser.setTimezone(user.getTimezone());
        createUser.setProfile(user.getProfile());

        return createUser;
    }
}
