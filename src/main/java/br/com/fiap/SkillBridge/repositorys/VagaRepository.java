package br.com.fiap.SkillBridge.repositorys;

import br.com.fiap.SkillBridge.models.Vaga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VagaRepository extends JpaRepository<Vaga, Long> {
    List<Vaga> findByEmpresaContainingIgnoreCase(String empresa);
    List<Vaga> findByTituloContainingIgnoreCase(String titulo);
}
