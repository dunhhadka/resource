package org.example.order.order.job.rabbitmq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.queue.PredicatedQueue;
import org.example.order.order.application.model.order.export.OrderExportRequest;
import org.example.order.order.infrastructure.configuration.messagebroker.RabbitMqConfiguration;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderJobHandler {

    private final RabbitTemplate rabbitTemplate;

    /**
     * @param storeId       id store
     * @param locationIds   location có quyển của current-user
     * @param exportRequest export request
     **/
    public void createExportJob(int storeId, List<Integer> locationIds, OrderExportRequest exportRequest) {
        rabbitTemplate.convertAndSend(RabbitMqConfiguration.QueueName.EXPORT_ORDER, new OrderJobDetail(storeId, locationIds, exportRequest));
    }

    public record OrderJobDetail(
            int storeId,
            List<Integer> locationIds,
            OrderExportRequest request
    ) {
    }
}
