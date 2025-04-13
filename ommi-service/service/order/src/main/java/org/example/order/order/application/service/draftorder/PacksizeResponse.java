package org.example.order.order.application.service.draftorder;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PacksizeResponse {
    private List<Packsize> packsizes;
}
