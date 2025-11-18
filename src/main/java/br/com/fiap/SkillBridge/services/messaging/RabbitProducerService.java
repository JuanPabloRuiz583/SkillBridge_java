package br.com.fiap.SkillBridge.services.messaging;

import br.com.fiap.SkillBridge.events.VagaEventDto;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RabbitProducerService {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public RabbitProducerService(RabbitTemplate rabbitTemplate,
                                 @Value("${app.messaging.exchange}") String exchange,
                                 @Value("${app.messaging.routing-key}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public void sendVagaEvent(VagaEventDto event) {
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }
}
