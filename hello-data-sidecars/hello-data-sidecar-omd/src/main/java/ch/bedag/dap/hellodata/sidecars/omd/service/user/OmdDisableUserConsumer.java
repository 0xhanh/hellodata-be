package ch.bedag.dap.hellodata.sidecars.omd.service.user;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUserUpdate;
import ch.bedag.dap.hellodata.sidecars.omd.client.OmdGatewayClient;
import ch.bedag.dap.hellodata.sidecars.omd.service.provider.OmdGatewayClientProvider;
import ch.bedag.dap.hellodata.sidecars.omd.service.resource.OmdUserResourceProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.DISABLE_USER;

@Log4j2
@Service
@RequiredArgsConstructor
@SuppressWarnings("java:S3516")
public class OmdDisableUserConsumer {
    private final OmdGatewayClientProvider omdClientProvider;
    private final OmdUserResourceProviderService userResourceProviderService;

    @SuppressWarnings("unused")
    @JetStreamSubscribe(event = DISABLE_USER, asyncRun = false)
    public void disableUser(SubsystemUserUpdate subsystemUserUpdate) {
        try {
            log.info("------- Received OMD user disable request {}", subsystemUserUpdate);

            OmdGatewayClient omdClient = omdClientProvider.getOmdGatewayClientInstance();
            if (OmdUserUtil.isAdmin(subsystemUserUpdate.getUsername())){
                log.info("User {} is admin, omitting. Email: {}", subsystemUserUpdate.getUsername(), subsystemUserUpdate.getEmail());
                return;
            }
            log.info("Going to disable user {} with email: {}", subsystemUserUpdate.getUsername(), subsystemUserUpdate.getEmail());
            omdClient.deleteUser(subsystemUserUpdate.getUsername(), subsystemUserUpdate.getEmail());
            userResourceProviderService.publishUsers();
        } catch (Exception e) {
            log.error("Could not disable user {}", subsystemUserUpdate.getEmail(), e);
        }
    }
}
