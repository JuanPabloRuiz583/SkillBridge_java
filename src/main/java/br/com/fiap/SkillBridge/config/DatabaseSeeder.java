package br.com.fiap.SkillBridge.config;

import br.com.fiap.SkillBridge.models.Vaga;
import br.com.fiap.SkillBridge.repositorys.VagaRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ============================================================
 *  COMPONENTE: DatabaseSeeder
 * ============================================================
 * Responsável por popular a base de dados com registros
 * iniciais (seed) para a entidade {@link Vaga}.
 *
 * Quando a aplicação sobe:
 * - Verifica se a tabela de vagas está vazia.
 * - Caso esteja, insere algumas vagas de exemplo para
 *   facilitar testes da SkillBridge (listagens, filtros, etc.).
 *
 * Observação:
 * - Em um cenário real, esse seeder poderia ser controlado por
 *   perfil (@Profile("dev")) para rodar apenas em ambiente de
 *   desenvolvimento.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSeeder {

    // =========================================================
    //  DEPENDÊNCIAS
    // =========================================================
    /**
     * Repositório responsável pelas operações de CRUD em TB_VAGA.
     *
     * Injeção via construtor (@RequiredArgsConstructor) é a forma
     * recomendada no Spring moderno, evitando o uso de @Autowired
     * em campo e facilitando testes.
     */
    private final VagaRepository vagaRepository;

    // =========================================================
    //  MÉTODO DE INICIALIZAÇÃO (SEED)
    // =========================================================
    /**
     * Executado automaticamente após o contexto Spring ser inicializado.
     *
     * Lógica:
     * - Se já houver vagas cadastradas, nenhum seed é aplicado.
     * - Se não houver, são criadas 3 vagas de exemplo.
     */
    @PostConstruct
    public void init() {
        long totalVagas = vagaRepository.count();

        if (totalVagas > 0) {
            log.info("[DatabaseSeeder] TB_VAGA já possui {} registro(s). Seed não será executado.", totalVagas);
            return;
        }

        log.info("[DatabaseSeeder] TB_VAGA está vazia. Iniciando carga inicial de vagas...");

        // -----------------------------------------------------
        //  Construção das vagas de exemplo
        // -----------------------------------------------------
        Vaga v1 = Vaga.builder()
                .titulo("Desenvolvedor Java Pleno")
                .empresa("Tech Solutions")
                .local("São Paulo, SP - Híbrido")
                .requisitos("Java 11+, Spring Boot, REST, SQL")
                .build();

        Vaga v2 = Vaga.builder()
                .titulo("Frontend React Developer")
                .empresa("UI Labs")
                .local("Remoto")
                .requisitos("React, TypeScript, Tailwind/DaisyUI, testes")
                .build();

        Vaga v3 = Vaga.builder()
                .titulo("Analista de Dados Jr.")
                .empresa("DataCorp")
                .local("Campinas, SP - Presencial")
                .requisitos("SQL, Python, ETL básicos, Power BI")
                .build();

        List<Vaga> vagas = List.of(v1, v2, v3);

        // -----------------------------------------------------
        //  Persistência em lote
        // -----------------------------------------------------
        vagaRepository.saveAll(vagas);

        log.info("[DatabaseSeeder] {} vaga(s) criada(s) com sucesso na TB_VAGA.", vagas.size());
    }
}
