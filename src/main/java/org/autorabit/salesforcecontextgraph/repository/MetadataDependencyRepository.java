package org.autorabit.salesforcecontextgraph.repository;


import org.autorabit.salesforcecontextgraph.db_entities.MetadataDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MetadataDependencyRepository extends JpaRepository<MetadataDependency, Long> {

    List<MetadataDependency> findByOrgIdAndMetadataTypeAndMetadataNameIn(
            String orgId,
            String metadataType,
            Collection<String> metadataNames
    );

    List<MetadataDependency> findByOrgId(String orgId);

}
