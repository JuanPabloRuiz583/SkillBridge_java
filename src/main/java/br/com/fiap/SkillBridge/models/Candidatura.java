package br.com.fiap.SkillBridge.models;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Entity
@Table(name = "candidatura")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"vaga"})
public class Candidatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "vagaId é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vaga_id", nullable = false)
    private Vaga vaga;

    @NotBlank(message = "nome é obrigatório")
    @Size(max = 150)
    @Column(length = 150, nullable = false)
    private String nome;

    @NotBlank(message = "email é obrigatório")
    @Email
    @Size(max = 200)
    @Column(length = 200, nullable = false)
    private String email;

    @NotBlank(message = "telefone é obrigatório")
    @Pattern(regexp = "^[0-9+()\\-\\s]*$", message = "Telefone contém caracteres inválidos")
    @Size(max = 50)
    @Column(length = 50, nullable = false)
    private String telefone;

    @Lob
    @NotBlank(message = "curriculo é obrigatório")
    @Size(max = 10000)
    @Column(nullable = false)
    private String curriculo;

    @NotBlank(message = "status é obrigatório")
    @Size(max = 50)
    @Column(length = 50, nullable = false)
    private String status = "PENDENTE";

    @PastOrPresent
    @NotNull(message = "dataAplicacao é obrigatório")
    @Column(name = "data_aplicacao", nullable = false)
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime dataAplicacao = LocalDateTime.now();
}