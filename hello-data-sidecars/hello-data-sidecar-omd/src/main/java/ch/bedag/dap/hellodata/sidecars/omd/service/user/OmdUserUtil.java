package ch.bedag.dap.hellodata.sidecars.omd.service.user;

import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUserUpdate;
import ch.bedag.dap.hellodata.sidecars.omd.client.user.response.OmdUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.openmetadata.client.model.CreateUser;

@Log4j2
@UtilityClass
public class OmdUserUtil {
    public static final String ADMIN_USER = "admin";

    public static OmdUser toOmdUser(SubsystemUserUpdate userUpdate) {
        OmdUser omdUser = new OmdUser();
        omdUser.setEmail(userUpdate.getEmail());
        omdUser.setFirstName(userUpdate.getFirstName());
        omdUser.setLastName(userUpdate.getLastName());
        omdUser.setUsername(userUpdate.getUsername());
        omdUser.setPassword(userUpdate.getPassword());
        return omdUser;
    }

    public static boolean isAdmin(String username) {
        return ADMIN_USER.equalsIgnoreCase(username);
    }

    public static String serialize(CreateUser createUser) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(createUser);
    }

    public static CreateUser deserialize(String json) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, CreateUser.class);
    }
}
