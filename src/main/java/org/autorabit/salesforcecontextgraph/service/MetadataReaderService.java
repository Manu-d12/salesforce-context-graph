package org.autorabit.salesforcecontextgraph.service;

import com.sforce.soap.metadata.DescribeMetadataResult;
import com.sforce.soap.metadata.FileProperties;
import com.sforce.soap.metadata.Metadata;
import java.util.List;
import java.util.Map;
import org.autorabit.salesforcecontextgraph.api.request.MetadataDescribeRequestDto;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.CustomStandardObjectDependencyCollector;
import org.autorabit.salesforcecontextgraph.integration.salesforce.MetadataApiClient;
import org.autorabit.salesforcecontextgraph.integration.salesforce.SalesforceSession;
import org.springframework.stereotype.Service;

@Service
public class MetadataReaderService {

    private final CustomStandardObjectDependencyCollector collector;
    private final MetadataApiClient metadataApiClient;

    public MetadataReaderService(
            CustomStandardObjectDependencyCollector collector,
            MetadataApiClient metadataApiClient
    ) {
        this.collector = collector;
        this.metadataApiClient = metadataApiClient;
    }

    public List<String> listMetadataObjects(String metadataType) {
        return collector.listMetadataFullNames(metadataType);
    }

    public List<String> listMetadataObjects(String metadataType, SalesforceSession session) {
        return collector.listMetadataFullNames(metadataType, session);
    }

    public List<MetadataApiClient.MetadataIdentifier> listMetadataIdentifiers(String metadataType) {
        return listMetadataIdentifiers(metadataType, null);
    }

    public List<MetadataApiClient.MetadataIdentifier> listMetadataIdentifiers(
            String metadataType,
            SalesforceSession session
    ) {
        return metadataApiClient.listMetadataIdentifiers(metadataType, session);
    }

    public DescribeMetadataResult describeMetadata() {
        return collector.describeMetadata();
    }

    public DescribeMetadataResult describeMetadata(SalesforceSession session) {
        return collector.describeMetadata(session);
    }

    public List<Map<String, Object>> getFieldDefinitions(List<String> fieldApiNames) {
        return collector.getFieldDefinitions(fieldApiNames);
    }

    public List<Map<String, Object>> getFieldDefinitions(List<String> fieldApiNames, SalesforceSession session) {
        return collector.getFieldDefinitions(fieldApiNames, session);
    }

    public List<Metadata> getMetaDataDescribe(MetadataDescribeRequestDto requestDto) {
        return getMetaDataDescribe(requestDto, null);
    }

    public List<Metadata> getMetaDataDescribe(MetadataDescribeRequestDto requestDto, SalesforceSession session) {
        List<String> normalizedNames = requestDto == null
                ? List.of()
                : requestDto.metadataApiNames().stream()
                        .filter(value -> value != null && !value.isBlank())
                        .map(String::trim)
                        .distinct()
                        .toList();
        if (normalizedNames.isEmpty()) {
            throw new IllegalArgumentException("metadataAPINames is required");
        }
        return metadataApiClient.getPermissionSetDescribe(normalizedNames, requestDto.metadataType(), session);
    }

    public List<FileProperties> listMetadataIdentifiersFileProperties(String metadataType, SalesforceSession session) {
        return metadataApiClient.listMetadataIdentifiersFileProperties(metadataType, session);
    }
}
