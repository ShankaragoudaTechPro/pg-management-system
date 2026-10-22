package com.pgmanager.pgmanagement.entity;

import lombok.*;

import jakarta.persistence.*;

@Entity
@Table(
        name = "facilities",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"owner_id", "name"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Facility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * NULL = Global/System facility
     *
     * Non-null = Owner custom facility
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Owner owner;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private boolean active;

    @PrePersist
    public void onCreate() {
        active = true;
    }
}