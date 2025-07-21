/*
 * Copyright © 2024, Kanton Bern
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ch.bedag.dap.hellodata.sidecars.omd.service.user;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUserUpdate;
import ch.bedag.dap.hellodata.sidecars.omd.client.OmdGatewayClient;
import ch.bedag.dap.hellodata.sidecars.omd.service.provider.OmdGatewayClientProvider;
import ch.bedag.dap.hellodata.sidecars.omd.service.resource.OmdUserResourceProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.openmetadata.client.model.CreateUser;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.CREATE_USER;

@Log4j2
@Service
@RequiredArgsConstructor
@SuppressWarnings("java:S3516")
public class OmdCreateUserConsumer {

    private final OmdUserResourceProviderService userResourceProviderService;
    private final OmdGatewayClientProvider omdClientProvider;

    /**
     * create_user:
     * {"roles":null,"email":"hanhvd@bedag.ch","password":null,"username":"hanhvd@bedag.ch","active":true,
     * "sendBackUsersList":false,"first_name":"Vũ","last_name":"Hanh"}
     * @param userCreate
     */
    @SuppressWarnings("unused")
    @JetStreamSubscribe(event = CREATE_USER, asyncRun = false)
    public void createUser(SubsystemUserUpdate userCreate) {
        try {
            log.info("------- Received Omd user creation request {}", userCreate);

            OmdGatewayClient omdClient = omdClientProvider.getOmdGatewayClientInstance();
            boolean existed = omdClient.checkEmailInUse(userCreate.getEmail());

            if (existed) {
                log.debug("User {} already exists in instance, omitting creation. Email: {}", userCreate.getUsername(), userCreate.getEmail());
                return;
            }

            log.info("Going to create new user with email: {}", userCreate.getEmail());
            CreateUser omdUser = toOmdUser(userCreate);
            omdClient.createUser(omdUser);
            if (userCreate.isSendBackUsersList()) {
                userResourceProviderService.publishUsers();
            }
        } catch (Exception e) {
            log.error("Could not create user {}", userCreate.getEmail(), e);
        }
    }

    public CreateUser toOmdUser(SubsystemUserUpdate userCreate) {
        CreateUser user = new CreateUser();
        user.setEmail(userCreate.getEmail());
        user.displayName(String.format("%s %s", userCreate.getFirstName(), userCreate.getLastName()));
        user.setIsBot(false);
        user.setIsAdmin(false);
        user.setName(userCreate.getUsername());
        user.description("");
        user.setRoles(new ArrayList<>());
        return user;
    }
}
