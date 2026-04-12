package org.autorabit.salesforcecontextgraph.ChatBot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request model for Salesforce deployment error analysis
 */

@Builder
public record DeploymentErrorRequest(String errorMessage, String componentName, String deploymentStatus) {
}

