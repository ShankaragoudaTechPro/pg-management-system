package com.pgmanager.pgmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "pg_facilities",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"pg_id", "facility_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PgFacility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pg_id", nullable = false)
    private PgProperty pg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;
}