package br.com.fiap.SkillBridge.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de saída para exibir vagas na interface (Thymeleaf)
 * ou em respostas de APIs REST.
 *
 * ➜ Papel:
 *   - Representa a visão "de leitura" da vaga para o cliente.
 *   - Usado em listagens, detalhamento e retornos JSON.
 *
 * ➜ Observação:
 *   - Campos aqui não devem representar regras de negócio complexas,
 *     apenas o que será exibido/repassado ao frontend.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VagaResponse {

    /**
     * Identificador único da vaga.
     */
    private Long id;

    /**
     * Título da vaga (ex.: "Desenvolvedor Java Júnior").
     */
    private String titulo;

    /**
     * Nome da empresa contratante.
     */
    private String empresa;

    /**
     * Local de trabalho (presencial, remoto, cidade/estado, etc.).
     */
    private String local;

    /**
     * Resumo dos requisitos principais da vaga.
     */
    private String requisitos;
}
