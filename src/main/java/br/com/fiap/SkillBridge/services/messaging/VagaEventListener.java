package br.com.fiap.SkillBridge.services.messaging;

import br.com.fiap.SkillBridge.events.VagaEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.rabbit.enabled", havingValue = "true", matchIfMissing = true)
public class VagaEventListener {
    private static final Logger log = LoggerFactory.getLogger(VagaEventListener.class);

    @RabbitListener(queues = "${app.messaging.queue}")
    public void handleVagaEvent(VagaEventDto event) {
        // lógica assíncrona: atualizar índice, notificar outro serviço, etc.
        log.info("Evento Vaga recebido: id={}, action={}, ts={}", event.getId(), event.getAction(), event.getTimestamp());
        // processar conforme necessidade
    }
}