package org.example.product.product.application.service.inventory;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserError {
    private String code;
    private String message;
    private List<String> fields;
}
