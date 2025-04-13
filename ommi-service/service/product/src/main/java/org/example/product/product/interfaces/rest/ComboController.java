package org.example.product.product.interfaces.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.product.product.application.model.combo.ComboRequest;
import org.example.product.product.application.service.combo.ComboService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/combos")
public class ComboController {

    private final ComboService comboService;

    @PostMapping
    public void create(@Valid @RequestBody ComboRequest request) {
        int storeId = 1;
        comboService.create(storeId, request);
    }
}
