package br.com.maisprati.projeto.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@DiscriminatorValue("CREDIT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CashbackCredit extends Transaction {
    @Column(name = "date_time_expiration")
    private Instant dateTimeExpiration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_point_id")
    private CollectionPoint collectionPoint;

    @Column(name = "cashback_percentage", precision = 5, scale = 2)
    private BigDecimal cashbackPercentage;
}
