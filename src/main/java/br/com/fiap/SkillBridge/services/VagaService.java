package br.com.fiap.SkillBridge.services;

import br.com.fiap.SkillBridge.events.VagaEventDto;
import br.com.fiap.SkillBridge.models.Vaga;
import br.com.fiap.SkillBridge.repositorys.VagaRepository;
import br.com.fiap.SkillBridge.services.messaging.RabbitProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Camada de serviço para regras de negócio relacionadas à entidade Vaga.
 *
 * Responsabilidades:
 *  - Orquestrar operações de CRUD sobre vagas.
 *  - Aplicar caching em consultas de listagem.
 *  - Centralizar validações simples antes de chamar o repositório.
 *
 * Observação:
 *  - Os métodos expõem/consomem a entidade Vaga diretamente.
 *    A camada de controller pode fazer o mapeamento entre DTOs
 *    (VagaRequest / VagaResponse) e a entidade.
 */
@Service
public class VagaService {

    private static final Logger log = LoggerFactory.getLogger(VagaService.class);

    private final VagaRepository vagaRepository;
    private final Optional<RabbitProducerService> rabbitProducer;
    //private final RabbitProducerService rabbitProducer;
    //RabbitProducerService rabbitProducer

    public VagaService(VagaRepository vagaRepository,  Optional<RabbitProducerService> rabbitProducer) {
        this.vagaRepository = vagaRepository;
        this.rabbitProducer = rabbitProducer;
    }

    // =========================================================================
    // 1. Consultas (com cache)
    // =========================================================================

    /**
     * Retorna todas as vagas cadastradas.
     *
     * Resultado é armazenado em cache ("vagas") para reduzir hits no banco.
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "vagas")
    public List<Vaga> getAllVagas() {
        log.debug("Buscando todas as vagas no banco (cache MISS)");
        return vagaRepository.findAll();
    }

    /**
     * Busca vaga por id, lançando exceção se não existir.
     */
    @Transactional(readOnly = true)
    public Vaga findById(Long id) {
        log.debug("Buscando vaga por id={}", id);
        return vagaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vaga não encontrada para o id " + id));
    }

    /**
     * Pesquisa vagas filtrando por nome da empresa (case-insensitive).
     */
    @Transactional(readOnly = true)
    public List<Vaga> findByEmpresaContainingIgnoreCase(String empresa) {
        log.debug("Buscando vagas por empresa contendo: '{}'", empresa);
        return vagaRepository.findByEmpresaContainingIgnoreCase(empresa);
    }

    /**
     * Pesquisa vagas filtrando por título (case-insensitive).
     */
    @Transactional(readOnly = true)
    public List<Vaga> findByTituloContainingIgnoreCase(String titulo) {
        log.debug("Buscando vagas por título contendo: '{}'", titulo);
        return vagaRepository.findByTituloContainingIgnoreCase(titulo);
    }

    // =========================================================================
    // 2. Escrita (criação / atualização / exclusão) com invalidação de cache
    // =========================================================================

    /**
     * Cria uma nova vaga.
     *
     * Regras:
     *  - Se o id vier preenchido e já existir no banco → lança exceção.
     *  - Ao salvar/alterar dados, o cache "vagas" é invalidado.
     */
    @Transactional
    @CacheEvict(cacheNames = "vagas", allEntries = true)
    public Vaga save(Vaga vaga) {
        if (vaga.getId() != null && vagaRepository.existsById(vaga.getId())) {
            log.warn("Tentativa de salvar vaga com id já existente: {}", vaga.getId());
            throw new RuntimeException("Id de vaga já cadastrado: " + vaga.getId());
        }

        Vaga saved = vagaRepository.save(vaga);
        rabbitProducer.ifPresent(p -> p.sendVagaEvent(new VagaEventDto(saved.getId(), "CREATED")));
        log.info("Vaga criada com sucesso. id={}", saved.getId());
        return saved;
    }

    /**
     * Atualiza uma vaga existente.
     *
     * Regras:
     *  - Verifica se a vaga existe antes de atualizar.
     *  - Copia campos editáveis (título, requisitos, empresa, local).
     *  - Invalida o cache de listagem "vagas".
     */
    @Transactional
    @CacheEvict(cacheNames = "vagas", allEntries = true)
    public Vaga update(Long id, Vaga vaga) {
        Vaga existente = vagaRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Tentativa de atualizar vaga inexistente. id={}", id);
                    return new RuntimeException("Vaga não encontrada para o id " + id);
                });

        existente.setTitulo(vaga.getTitulo());
        existente.setRequisitos(vaga.getRequisitos());
        existente.setEmpresa(vaga.getEmpresa());
        existente.setLocal(vaga.getLocal());

        Vaga updated = vagaRepository.save(existente);
        rabbitProducer.ifPresent(p -> p.sendVagaEvent(new VagaEventDto(updated.getId(), "UPDATED")));
        log.info("Vaga atualizada com sucesso. id={}", updated.getId());
        return updated;
    }

    /**
     * Exclui uma vaga pelo id.
     *
     * Regra:
     *  - Caso não exista, a chamada do repositório pode lançar exceção,
     *    que será tratada pelo handler global da aplicação.
     *  - Sempre invalida o cache de listagem.
     */
    @Transactional
    @CacheEvict(cacheNames = "vagas", allEntries = true)
    public void deleteById(Long id) {
        log.info("Excluindo vaga id={}", id);
        vagaRepository.deleteById(id);
        rabbitProducer.ifPresent(p -> p.sendVagaEvent(new VagaEventDto(id, "DELETED")));
    }
}
