package br.com.fiap.SkillBridge.services;
import br.com.fiap.SkillBridge.dto.CandidaturaDTO;
import br.com.fiap.SkillBridge.models.Candidatura;
import br.com.fiap.SkillBridge.models.Vaga;
import br.com.fiap.SkillBridge.repositorys.CandidaturaRepository;
import br.com.fiap.SkillBridge.repositorys.VagaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CandidaturaService {

    private final CandidaturaRepository candidaturaRepository;
    private final VagaRepository vagaRepository;

    @Transactional
    public CandidaturaDTO create(CandidaturaDTO dto) {
        Vaga vaga = vagaRepository.findById(dto.getVagaId())
                .orElseThrow(() -> new EntityNotFoundException("Vaga não encontrada"));
        Candidatura c = new Candidatura();
        c.setVaga(vaga);
        c.setNome(dto.getNome());
        c.setEmail(dto.getEmail());
        c.setTelefone(dto.getTelefone());
        c.setCurriculo(dto.getCurriculo());
        c.setStatus(dto.getStatus() != null ? dto.getStatus() : "PENDENTE");
        c.setDataAplicacao(dto.getDataAplicacao() != null ? dto.getDataAplicacao() : LocalDateTime.now());
        Candidatura saved = candidaturaRepository.save(c);
        return toDto(saved);
    }

    public CandidaturaDTO findById(Long id) {
        return candidaturaRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Candidatura não encontrada"));
    }

    public List<CandidaturaDTO> findAll() {
        return candidaturaRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public CandidaturaDTO update(Long id, CandidaturaDTO dto) {
        Candidatura existing = candidaturaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Candidatura não encontrada"));
        if (dto.getVagaId() != null && !dto.getVagaId().equals(existing.getVaga().getId())) {
            Vaga vaga = vagaRepository.findById(dto.getVagaId())
                    .orElseThrow(() -> new EntityNotFoundException("Vaga não encontrada"));
            existing.setVaga(vaga);
        }
        if (dto.getNome() != null) existing.setNome(dto.getNome());
        if (dto.getEmail() != null) existing.setEmail(dto.getEmail());
        if (dto.getTelefone() != null) existing.setTelefone(dto.getTelefone());
        if (dto.getCurriculo() != null) existing.setCurriculo(dto.getCurriculo());
        if (dto.getStatus() != null) existing.setStatus(dto.getStatus());
        if (dto.getDataAplicacao() != null) existing.setDataAplicacao(dto.getDataAplicacao());
        return toDto(candidaturaRepository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        if (!candidaturaRepository.existsById(id)) {
            throw new EntityNotFoundException("Candidatura não encontrada");
        }
        candidaturaRepository.deleteById(id);
    }

    private CandidaturaDTO toDto(Candidatura c) {
        return new CandidaturaDTO(
                c.getId(),
                c.getVaga() != null ? c.getVaga().getId() : null,
                c.getNome(),
                c.getEmail(),
                c.getTelefone(),
                c.getCurriculo(),
                c.getStatus(),
                c.getDataAplicacao()
        );
    }
}
