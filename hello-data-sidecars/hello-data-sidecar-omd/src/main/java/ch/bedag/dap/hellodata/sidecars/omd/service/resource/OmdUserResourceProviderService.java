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
package ch.bedag.dap.hellodata.sidecars.omd.service.resource;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.nats.service.NatsSenderService;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.UserResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemGetAllUsers;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemRole;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import ch.bedag.dap.hellodata.sidecars.omd.service.cloud.PodUtilsProvider;
import ch.bedag.dap.hellodata.sidecars.omd.service.provider.OmdGatewayClientProvider;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.openmetadata.client.model.EntityReference;
import org.openmetadata.client.model.User;
import org.openmetadata.client.model.UserList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.kubernetes.commons.PodUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.stream.IntStream;

import jakarta.validation.constraints.NotNull;
import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.GET_ALL_USERS;
import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.PUBLISH_USER_RESOURCES;

@Log4j2
@Service
@RequiredArgsConstructor
public class OmdUserResourceProviderService {

    private final NatsSenderService natsSenderService;
    private final OmdGatewayClientProvider omdClientProvider;
    private final PodUtilsProvider podUtilsProvider;
    @Value("${hello-data.instance.name}")
    private String instanceName;

    @JetStreamSubscribe(event = GET_ALL_USERS)
    public void refreshUsers(SubsystemGetAllUsers subsystemGetAllUsers) throws URISyntaxException, IOException {
        log.info("--> Publish all users event {}", subsystemGetAllUsers);
        publishUsers();
    }

    @Scheduled(fixedDelayString = "${hello-data.sidecar.publish-interval-minutes:10}", timeUnit = TimeUnit.MINUTES)
    public void publishUsers() throws URISyntaxException, IOException {
        log.info("--> publishUsers()");
        UserList response = omdClientProvider.getOmdGatewayClientInstance().users();

        PodUtils<V1Pod> podUtils = podUtilsProvider.getIfAvailable();
        List<User> omdUsers = CollectionUtils.emptyIfNull(response.getData()).stream().toList();
         List<SubsystemUser> subsystemUsers = toOmdUsers(omdUsers);
        if (podUtils != null) {
            V1Pod current = podUtils.currentPod().get();
            UserResource userResource = new UserResource(ModuleType.OPENDATAGOV, this.instanceName, current.getMetadata().getNamespace(), subsystemUsers);
            natsSenderService.publishMessageToJetStream(PUBLISH_USER_RESOURCES, userResource);
        } else {
            //dummy info for tests
            UserResource userResource = new UserResource(ModuleType.AIRFLOW, this.instanceName, "local", subsystemUsers);
            natsSenderService.publishMessageToJetStream(PUBLISH_USER_RESOURCES, userResource);
        }
    }

    private List<SubsystemUser> toOmdUsers(List<User> omdUsers) {
        if (omdUsers.isEmpty()) {
            return new ArrayList<>();
        }
        List<User> modifiableList = new ArrayList<>(omdUsers);
        modifiableList.sort(Comparator.comparing(User::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        return IntStream.range(0, modifiableList.size()).mapToObj(i -> toSubsystemUser(i + 2, modifiableList.get(i))).toList();
    }

    @NotNull
    private SubsystemUser toSubsystemUser(int index, User omdUser) {
        SubsystemUser subsystemUser = new SubsystemUser();
        subsystemUser.setId(index);
        // subsystemUser.setId(omdUser.getId().hashCode()); // truncted 32-bit int, NOT UNIQUE
        subsystemUser.setUsername(omdUser.getName());
        subsystemUser.setEmail(omdUser.getEmail());
        subsystemUser.setFirstName(omdUser.getName());
        subsystemUser.setLastName(omdUser.getName());
        subsystemUser.setActive(Boolean.FALSE.equals(omdUser.getDeleted()));
        subsystemUser.setRoles(toOmRoles((List<EntityReference>) CollectionUtils.emptyIfNull(omdUser.getRoles())));

        return subsystemUser;
    }

    private List<SubsystemRole> toOmRoles(List<EntityReference> omdUserRoles) {
        return omdUserRoles.stream().map(this::toOmdRole).toList();
    }

    private SubsystemRole toOmdRole(EntityReference omdUserRole) {
        SubsystemRole subsystemRole = new SubsystemRole();
        subsystemRole.setId(getRoleId(omdUserRole));
        subsystemRole.setName(omdUserRole.getName());
        return subsystemRole;
    }

    /**
     * Some small arbitrary mapping of airflow rolename to an Integer
     */
    private int getRoleId(EntityReference omUserRole) {
        return omUserRole.getName().equalsIgnoreCase("Admin") ? 1 : 2;
    }
}
