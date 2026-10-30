package br.com.maisprati.ecotecidos.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequestDTO (
        @NotBlank(message = "O nome da categoria é obrigatório.")
        @Size(min = 3, max = 50, message = "O nome da categoria deve possuir entre 3 e 50 caracteres.")
        @Schema(description = "Nome da categoria", example = "Upcycling")
        String name
){
}
