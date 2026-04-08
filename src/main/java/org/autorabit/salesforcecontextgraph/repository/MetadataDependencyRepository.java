package org.autorabit.salesforcecontextgraph.repository;


import org.autorabit.salesforcecontextgraph.db_entities.MetadataDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MetadataDependencyRepository extends JpaRepository<MetadataDependency, Long> {

}
