package org.example.order.order.infrastructure.data.dao;

import java.util.List;

public interface DiscountAllocationDao {
    List<DiscountAllocationDto> getByOrderId(int storeId, int orderId);
}
