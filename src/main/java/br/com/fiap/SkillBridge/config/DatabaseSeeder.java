package br.com.fiap.SkillBridge.config;

import br.com.fiap.SkillBridge.models.Vaga;
import br.com.fiap.SkillBridge.repositorys.VagaRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DatabaseSeeder {
    @Autowired
    private VagaRepository vagaRepository;

    @PostConstruct
    public void init() {
        if (vagaRepository.count() == 0) {
            Vaga v1 = new Vaga();
            v1.setTitulo("Desenvolvedor Java Pleno");
            v1.setEmpresa("Tech Solutions");
            v1.setLocal("São Paulo, SP - Híbrido");
            v1.setRequisitos("Java 11+, Spring Boot, REST, SQL");

            Vaga v2 = new Vaga();
            v2.setTitulo("Frontend React Developer");
            v2.setEmpresa("UI Labs");
            v2.setLocal("Remoto");
            v2.setRequisitos("React, TypeScript, Tailwind/DaisyUI, testes");

            Vaga v3 = new Vaga();
            v3.setTitulo("Analista de Dados Jr.");
            v3.setEmpresa("DataCorp");
            v3.setLocal("Campinas, SP - Presencial");
            v3.setRequisitos("SQL, Python, ETL básicos, Power BI");

            vagaRepository.saveAll(List.of(v1, v2, v3));
        }
    }
}