package br.com.maisprati.ecotecidos.dto.response;

import java.util.Set;

public record CollectionPointUsersResponseDTO(
    CollectionPointSummaryResponseDTO collectionPoint,
    Set<UserSummaryResponseDTO> managers,
    Set<UserSummaryResponseDTO> operators
) {

}
