package ch.bedag.dap.hellodata.sidecars.omd.config;

import ch.bedag.dap.hellodata.sidecars.omd.client.OmdGatewayClient;
import ch.bedag.dap.hellodata.sidecars.omd.config.properties.OmdProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
@RequiredArgsConstructor
@Data
public class OmdConfig {
    private final OmdProperties omdProperties;

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public OmdGatewayClient omdGatewayClient() {
        return new OmdGatewayClient(omdProperties);
    }
}
