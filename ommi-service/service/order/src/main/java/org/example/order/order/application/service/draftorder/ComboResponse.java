package org.example.order.order.application.service.draftorder;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ComboResponse {
    private List<Combo> combos;
}
