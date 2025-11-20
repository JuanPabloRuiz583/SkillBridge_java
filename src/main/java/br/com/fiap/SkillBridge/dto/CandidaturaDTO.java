package br.com.fiap.SkillBridge.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CandidaturaDTO {

    private Long id;

    @NotNull(message = "Selecione uma vaga")
    private Long vagaId;

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 150)
    private String nome;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    @Size(max = 200)
    private String email;

    @NotBlank(message = "Telefone é obrigatório")
    @Size(max = 50)
    private String telefone;

    @NotBlank(message = "Currículo é obrigatório")
    @Size(max = 10000)
    private String curriculo;

    @NotBlank(message = "Status é obrigatório")
    private String status;

    private LocalDateTime dataAplicacao;
}
