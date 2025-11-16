package br.com.fiap.SkillBridge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VagaDto {
    private Long id;
    private String titulo;
    private String empresa;
    private String local;
    private String requisitos;
}