package br.com.fiap.SkillBridge.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de resposta do chat.
 *
 * ➜ Papel:
 *   - Representa a estrutura enviada do backend para o frontend
 *     após o processamento da mensagem do usuário.
 *
 * ➜ Uso:
 *   - Retorno do endpoint POST /chat/api no ChatController.
 *
 * ➜ Estrutura:
 *   - reply: texto gerado pela camada de “IA”/regras (AIService),
 *            já pronto para exibição na interface.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {

    /**
     * Resposta gerada pela IA / regra de negócio
     * a partir da mensagem enviada pelo usuário.
     *
     * Exposta para o frontend como campo JSON "reply".
     */
    private String reply;
}
