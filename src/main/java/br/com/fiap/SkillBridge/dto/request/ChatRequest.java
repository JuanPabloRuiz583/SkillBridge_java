package br.com.fiap.SkillBridge.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de entrada utilizado para receber a mensagem
 * enviada pelo usuário no chat da aplicação SkillBridge.
 *
 * ➜ Fluxo típico:
 *   - Frontend envia JSON: { "message": "texto do usuário" }
 *   - Backend recebe em ChatRequest no endpoint POST /chat/api.
 *
 * ➜ Recursos:
 *   - Bean Validation (@NotBlank, @Size) para validar a mensagem.
 *   - Internacionalização (i18n) via chaves em messages.properties.
 *   - Lombok (@Data, @Builder, etc.) para reduzir boilerplate.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequest {

    /**
     * Texto digitado pelo usuário no chat.
     *
     * Regras:
     *  - Não pode ser vazio (@NotBlank)
     *  - Tamanho máximo de 1.000 caracteres para evitar payloads gigantes.
     *
     * Mensagens de erro utilizam chaves i18n:
     *  - {chat.message.not-blank}
     *  - {chat.message.size}
     *
     * Exemplo:
     *  "Quais vagas remotas tenho para desenvolvedor Java júnior?"
     */
    @NotBlank(message = "{chat.message.not-blank}")
    @Size(max = 1000, message = "{chat.message.size}")
    private String message;
}
