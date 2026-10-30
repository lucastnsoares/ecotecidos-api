package br.com.maisprati.ecotecidos.dto.response;

import br.com.maisprati.ecotecidos.dto.request.CollectionPointUpdateRequestDTO;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class PendingUpdateResponseDTO {
    private Long id; // id do update
    private CollectionPointResponseDTO currentPoint; // Estado atual do ponto
    private CollectionPointUpdateRequestDTO proposedChanges; // Dados propostos (Deserializados do JSON)
    private String requestedByEmail;
    private LocalDateTime requestedAt;
}