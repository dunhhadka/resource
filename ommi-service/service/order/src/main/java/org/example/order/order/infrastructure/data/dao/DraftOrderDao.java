package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.infrastructure.data.dto.DraftOrderDto;

import java.util.List;

public interface DraftOrderDao {

    List<DraftOrderDto> getForReIndexES(int startId, int take);

}
