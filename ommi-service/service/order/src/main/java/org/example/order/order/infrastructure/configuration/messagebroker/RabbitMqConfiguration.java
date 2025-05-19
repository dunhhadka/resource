package org.example.order.order.infrastructure.configuration.messagebroker;

import org.example.order.order.application.utils.JsonUtils;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class RabbitMqConfiguration {

    @Value("${spring.rabbitmq.listener.auto-startup}")
    private boolean autoStartup;

    @Value("${spring.rabbitmq.listener.concurrent}")
    private int concurrentConsumer;

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        var factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(concurrentConsumer);
        factory.setAutoStartup(autoStartup);
        factory.setMessageConverter(jsonMessageConverter());
        return factory;
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        var objectMapper = JsonUtils.createObjectMapper();
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitAdmin admin(ConnectionFactory factory) {
        var ra = new RabbitAdmin(factory);

        var queueNames = Arrays.asList(
                QueueName.EXPORT_DRAFT_ORDER,
                QueueName.EXPORT_ORDER,
                QueueName.BULK_ACTION_ORDER,
                QueueName.BULK_ACTION_DRAFT_ORDER,
                QueueName.BULK_ACTION_CHECKOUT,
                QueueName.BULK_ACTION_SHIPMENT,
                QueueName.SHIPMENT_SAPO_EXPRESS,
                QueueName.REINDEX_DRAFT_ORDER,
                QueueName.REINDEX_ORDER,
                QueueName.MIGRATE_DRAFT_ORDER_TAG
        );

        for (var queueName : queueNames) {
            ra.declareQueue(new Queue(queueName, true, false, false));
        }

        return ra;
    }

    public static class QueueName {
        public static final String EXPORT_ORDER = "sapo.omni.export.order";
        public static final String EXPORT_DRAFT_ORDER = "sapo.omni.export.draft_order";
        public static final String BULK_ACTION_ORDER = "sapo.omni.bulkaction.order";
        public static final String BULK_ACTION_DRAFT_ORDER = "sapo.omni.bulkaction.draft_order";
        public static final String REINDEX_DRAFT_ORDER = "sapo.omni.reindex.draft_order";
        public static final String REINDEX_ORDER = "sapo.omni.reindex.order";
        public static final String BULK_ACTION_CHECKOUT = "bulkaction.checkout";
        public static final String BULK_ACTION_SHIPMENT = "sapo.omni.bulkaction.shipment";
        public static final String SHIPMENT_SAPO_EXPRESS = "shipment.sapo_express";
        public static final String MIGRATE_DRAFT_ORDER_TAG = "sapo.omni.migrate.draft_order_tag";
    }
}
