package br.com.fiap.SkillBridge.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de entrada para criação/atualização de vagas na SkillBridge.
 *
 * ➜ Uso típico:
 *   - Enviado a partir de formulários Thymeleaf (MVC)
 *   - Ou como payload JSON em endpoints REST (POST/PUT de vaga)
 *
 * ➜ Papel:
 *   - Representa apenas os dados que o usuário pode informar/editar.
 *   - Evita expor detalhes internos da entidade de domínio.
 *
 * ➜ Validação:
 *   - Bean Validation garante obrigatoriedade e limites de tamanho.
 *   - Mensagens de erro usam chaves i18n em messages.properties.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VagaRequest {

    /**
     * Identificador da vaga.
     *
     * - Em criação, normalmente será null (gerado pelo banco / JPA).
     * - Em atualização (PUT/POST de edição), pode ser utilizado
     *   para vincular a alteração ao registro existente.
     */
    private Long id;

    /**
     * Título da vaga, por exemplo:
     *  - "Desenvolvedor Java Júnior"
     *  - "Analista de Dados Pleno"
     */
    @NotBlank(message = "{vaga.titulo.not-blank}")
    @Size(max = 200, message = "{vaga.titulo.size}")
    private String titulo;

    /**
     * Nome da empresa responsável pela vaga.
     * Exemplo: "FIAP Tech", "Empresa X".
     */
    @NotBlank(message = "{vaga.empresa.not-blank}")
    @Size(max = 150, message = "{vaga.empresa.size}")
    private String empresa;

    /**
     * Local da vaga, podendo ser:
     *  - Cidade/Estado (ex: "São Paulo - SP")
     *  - "Remoto", "Híbrido", etc.
     */
    @NotBlank(message = "{vaga.local.not-blank}")
    @Size(max = 150, message = "{vaga.local.size}")
    private String local;

    /**
     * Campo de texto para requisitos/responsabilidades.
     *
     * Exemplos:
     *  - "Java, Spring Boot, REST, PostgreSQL"
     *  - "Power BI, SQL, experiência com análise de dados"
     */
    @NotBlank(message = "{vaga.requisitos.not-blank}")
    @Size(max = 2000, message = "{vaga.requisitos.size}")
    private String requisitos;
}
