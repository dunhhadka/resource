package org.example.order.order.application.model.order.response;

import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.order.model.LineItem;

import java.util.List;

@Getter
@Setter
public class OrderResponse {

    private int id;

    private List<LineItem> lineItems;

}
