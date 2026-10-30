package br.com.maisprati.projeto.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cashback_rules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CashbackRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "points_per_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal pointsPerKg;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}