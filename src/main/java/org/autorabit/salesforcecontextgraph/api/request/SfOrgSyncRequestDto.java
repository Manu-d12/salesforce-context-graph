package org.autorabit.salesforcecontextgraph.api.request;

public record SfOrgSyncRequestDto(
        String loginUrl,
        String clientId,
        String clientSecret
) {
}
