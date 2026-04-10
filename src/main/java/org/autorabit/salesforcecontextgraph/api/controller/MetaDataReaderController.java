package org.autorabit.salesforcecontextgraph.api.controller;

import com.sforce.soap.metadata.DescribeMetadataResult;
import com.sforce.soap.metadata.Metadata;
import java.util.List;
import java.util.Map;
import org.autorabit.salesforcecontextgraph.api.request.FieldDefinitionsRequestDto;
import org.autorabit.salesforcecontextgraph.api.request.MetadataDescribeRequestDto;
import org.autorabit.salesforcecontextgraph.api.response.MetadataIdentifiersResponse;
import org.autorabit.salesforcecontextgraph.api.response.MetadataObjectsResponse;
import org.autorabit.salesforcecontextgraph.service.MetadataReaderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metadata-reader")
public class MetaDataReaderController {

    private final MetadataReaderService metadataReaderService;

    public MetaDataReaderController(MetadataReaderService metadataReaderService) {
        this.metadataReaderService = metadataReaderService;
    }

    @GetMapping("/{metadataType}")
    public MetadataObjectsResponse listMetadataObjects(@PathVariable String metadataType) {
        return new MetadataObjectsResponse(metadataReaderService.listMetadataObjects(metadataType));
    }

    @GetMapping("/{metadataType}/identifiers")
    public MetadataIdentifiersResponse listMetadataIdentifiers(@PathVariable String metadataType) {
        return new MetadataIdentifiersResponse(metadataReaderService.listMetadataIdentifiers(metadataType));
    }

    @GetMapping("/describe")
    public DescribeMetadataResult describeMetadata() {
        return metadataReaderService.describeMetadata();
    }

    @PostMapping("/field-definitions")
    public List<Map<String, Object>> getFieldDefinitions(@RequestBody FieldDefinitionsRequestDto requestDto) {
        if (requestDto == null || requestDto.fieldApiNames() == null || requestDto.fieldApiNames().isEmpty()) {
            throw new IllegalArgumentException("fieldApiNames is required");
        }
        return metadataReaderService.getFieldDefinitions(requestDto.fieldApiNames());
    }

    @PostMapping("/meta-data/describe")
    public List<Metadata> getMetaDataDescribe(@RequestBody MetadataDescribeRequestDto requestDto) {
        if (requestDto == null) {
            throw new IllegalArgumentException("metadataNames and type is required");
        }
        return metadataReaderService.getMetaDataDescribe(requestDto);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleErrors(RuntimeException ex) {
        return Map.of("error", ex.getMessage());
    }
}
