package org.example.order.order.domain.order.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class App {
    private int id;
    private String key;
    private String alias;
    private String title;
}
