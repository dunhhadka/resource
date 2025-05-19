package org.example.order.order.application.service.order;

import lombok.RequiredArgsConstructor;
import org.example.order.order.application.model.order.export.OrderExportRequest;
import org.example.order.order.application.model.order.response.OrderResponse;
import org.example.order.order.job.rabbitmq.OrderExportJob;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderReadService {

    public List<OrderResponse> search(int storeId, OrderExportJob.LocationPermission locationPermission, OrderExportRequest filterRequest, String searchType) {

        return List.of();
    }
}
