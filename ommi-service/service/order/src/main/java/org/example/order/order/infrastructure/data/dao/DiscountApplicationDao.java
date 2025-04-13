package org.example.order.order.infrastructure.data.dao;

import java.util.List;

public interface DiscountApplicationDao {
    List<DiscountApplicationDto> getByOrderId(int storeId, int orderId);
}
