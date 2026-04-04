package org.autorabit.salesforcecontextgraph.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SalesforceIntegrationProperties.class)
public class SalesforceConfiguration {
}
