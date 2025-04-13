package org.example.order.order.infrastructure.data.dao;

import java.util.List;

public interface RefundTaxLineDao {
    List<RefundTaxLineDto> getByOrderId(int storeId, int orderId);
}
