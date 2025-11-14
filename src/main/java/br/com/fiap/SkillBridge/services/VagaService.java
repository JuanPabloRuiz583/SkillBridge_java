package br.com.fiap.SkillBridge.services;

import br.com.fiap.SkillBridge.models.Vaga;
import br.com.fiap.SkillBridge.repositorys.VagaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VagaService {

    private final VagaRepository vagaRepository;

    public VagaService(VagaRepository vagaRepository) {
        this.vagaRepository = vagaRepository;
    }

    public List<Vaga> getAllVagas() {
        return vagaRepository.findAll();
    }

    public Vaga save(Vaga vaga) {
        if (vaga.getId() != null && vagaRepository.existsById(vaga.getId())) {
            throw new RuntimeException("Id já cadastrado.");
        }
        return vagaRepository.save(vaga);
    }

    public Vaga update(Long id, Vaga vaga) {
        Vaga existente = vagaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vaga não encontrada"));
        existente.setTitulo(vaga.getTitulo());
        existente.setRequisitos(vaga.getRequisitos());
        existente.setEmpresa(vaga.getEmpresa());
        existente.setLocal(vaga.getLocal());
        return vagaRepository.save(existente);
    }

    public void deleteById(Long id) {
        vagaRepository.deleteById(id);
    }

    public Vaga findById(Long id) {
        return vagaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vaga não encontrada"));
    }

    public List<Vaga> findByEmpresaContainingIgnoreCase(String empresa) {
        return vagaRepository.findByEmpresaContainingIgnoreCase(empresa);
    }

    public List<Vaga> findByTituloContainingIgnoreCase(String titulo) {
        return vagaRepository.findByTituloContainingIgnoreCase(titulo);
    }
}
