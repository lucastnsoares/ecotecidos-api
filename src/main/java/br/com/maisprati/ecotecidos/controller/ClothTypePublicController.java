package br.com.maisprati.ecotecidos.controller;

import br.com.maisprati.ecotecidos.dto.response.ClothTypeResponseDTO;
import br.com.maisprati.ecotecidos.service.ClothTypeService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cloth-types")
@RequiredArgsConstructor
public class ClothTypePublicController {

    private final ClothTypeService clothTypeService;

    @GetMapping
    public ResponseEntity<Page<ClothTypeResponseDTO>> getClothTypes(
            @ParameterObject
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "name",
                    direction = Sort.Direction.ASC
            )
            Pageable pageable) {

        return ResponseEntity.ok(clothTypeService.findAll(pageable));
    }
}
