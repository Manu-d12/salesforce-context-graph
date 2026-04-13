package org.autorabit.salesforcecontextgraph.db_entities;

import jakarta.persistence.*;
import lombok.*;
import org.autorabit.salesforcecontextgraph.domain.enums.JobStatus;

import java.util.Date;

@Entity
@Table(name = "sync_job")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SyncJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "sf_org_id")
    private String orgId;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "status")
    private JobStatus status;


    @Column(name = "created_date")
    Date createdDate;
}
