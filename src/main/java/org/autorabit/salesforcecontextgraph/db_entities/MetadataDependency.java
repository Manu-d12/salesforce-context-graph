package org.autorabit.salesforcecontextgraph.db_entities;


import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "metadata_dependency")
@Entity
@Builder
public class MetadataDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id")
    private String orgId;

    @Column(name = "metadata_type")
    private String metadataType;

    @Column(name = "metadata_name")
    private String metadataName;

    @Column(name = "metadata_label")
    private String metadataLabel;

    @Column(name = "ref_metadata_type")
    private String refMetadataType;

    @Column(name = "ref_metadata_name")
    private String refMetadataName;

    @Column(name = "ref_metadata_label")
    private String refMetadataLabel;

    @Column(name = "edge_type")
    private String edgeType;
}
