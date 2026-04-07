package org.autorabit.salesforcecontextgraph.service;

import com.sforce.soap.metadata.DescribeMetadataResult;
import com.sforce.soap.metadata.Metadata;
import com.sforce.soap.metadata.PermissionSet;
import java.util.List;
import java.util.Map;

import org.autorabit.salesforcecontextgraph.api.request.MetadataDescribeRequestDto;
import org.autorabit.salesforcecontextgraph.collectorserviceimpl.CustomStandardObjectDependencyCollector;
import org.autorabit.salesforcecontextgraph.integration.salesforce.MetadataApiClient;
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

    public DescribeMetadataResult describeMetadata() {
        return collector.describeMetadata();
    }

    public List<Map<String, Object>> getFieldDefinitions(List<String> fieldApiNames) {
        return collector.getFieldDefinitions(fieldApiNames);
    }

    public List<Metadata> getMetaDataDescribe(MetadataDescribeRequestDto requestDto) {
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
        return metadataApiClient.getPermissionSetDescribe(normalizedNames, requestDto.metadataType());
    }
}
