package org.example.order.order.domain.order.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Channel {
    private int id;
    private String mainName;
    private String subName;
    private String alias;
}
