package br.com.maisprati.projeto.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("DEBIT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CashbackDebit extends Transaction {

    @Column(name = "partner_store")
    private String partnerStore;
}
