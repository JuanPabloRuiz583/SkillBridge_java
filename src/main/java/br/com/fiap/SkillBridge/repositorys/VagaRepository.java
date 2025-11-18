package br.com.fiap.SkillBridge.repositorys;

import br.com.fiap.SkillBridge.models.Vaga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ============================================================
 *  REPOSITÓRIO: VagaRepository
 * ============================================================
 * Interface responsável por acessar a tabela TB_VAGA no banco
 * de dados através do Spring Data JPA.
 *
 * Funcionalidades principais:
 * - Herda de {@link JpaRepository}, recebendo automaticamente
 *   operações CRUD básicas:
 *   - save, saveAll
 *   - findById, findAll
 *   - deleteById, delete, deleteAll
 *   - count, existsById
 *
 * - Define métodos de consulta específicos baseados na
 *   convenção de nomes do Spring Data (query methods),
 *   permitindo buscas por empresa e título de forma simples.
 */
@Repository
public interface VagaRepository extends JpaRepository<Vaga, Long> {

    // =========================================================
    //  CONSULTAS PERSONALIZADAS
    // =========================================================

    /**
     * Busca vagas filtrando pelo nome da empresa, ignorando
     * diferenças de maiúsculas e minúsculas.
     *
     * Exemplo de uso:
     * - findByEmpresaContainingIgnoreCase("tech")
     *   → retorna vagas de "Tech Solutions", "Tech Corp", etc.
     *
     * Palavra-chave:
     * - "Containing" → faz um LIKE '%termo%'
     * - "IgnoreCase" → converte para mesma caixa ao comparar
     */
    List<Vaga> findByEmpresaContainingIgnoreCase(String empresa);

    /**
     * Busca vagas filtrando pelo título, ignorando diferenças
     * de maiúsculas e minúsculas.
     *
     * Exemplo de uso:
     * - findByTituloContainingIgnoreCase("desenvolvedor")
     *   → retorna "Desenvolvedor Java Pleno",
     *     "Desenvolvedor Backend", etc.
     *
     * Útil para:
     * - Implementar busca textual em endpoints de listagem
     *   de vagas da SkillBridge.
     */
    List<Vaga> findByTituloContainingIgnoreCase(String titulo);
}
