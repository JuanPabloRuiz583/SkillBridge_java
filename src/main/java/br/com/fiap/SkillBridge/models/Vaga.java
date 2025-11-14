package br.com.fiap.SkillBridge.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Data
public class Vaga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Título da vaga é obrigatório")
    @Size(max = 120, message = "Título não pode exceder 120 caracteres")
    private String titulo;

    @NotBlank(message = "Requisitos são obrigatórios")
    @Size(max = 300, message = "Requisitos não podem exceder 300 caracteres")
    private String requisitos;

    @NotBlank(message = "Nome da empresa é obrigatório")
    @Size(max = 100, message = "Nome da empresa não pode exceder 100 caracteres")
    private String empresa;

    @NotBlank(message = "Local é obrigatório")
    @Size(max = 200, message = "Local não pode exceder 200 caracteres")
    private String local;
}
