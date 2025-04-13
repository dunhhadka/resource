package org.example.order.order.infrastructure.data.dao;

import java.util.List;

public interface TaxLineDao {
    List<TaxLineDto> getByOrderId(int storeId, int orderId);
}
