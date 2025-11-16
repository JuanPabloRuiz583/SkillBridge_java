package br.com.fiap.SkillBridge.tools;

import br.com.fiap.SkillBridge.models.Vaga;
import br.com.fiap.SkillBridge.services.VagaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import br.com.fiap.SkillBridge.dto.VagaDto;

import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Component
public class VagaTool {

    private static final Logger log = LoggerFactory.getLogger(VagaTool.class);
    private final VagaService vagaService;

    public VagaTool(VagaService vagaService) {
        this.vagaService = vagaService;
    }

    public List<VagaDto> searchVagas(String query) {
        String term = extractSearchTerm(query);
        log.info("searchVagas - query='{}' -> term='{}'", query, term);

        if (term == null || term.isBlank()) {
            log.info("searchVagas - termo vazio após normalização");
            return Collections.emptyList();
        }

        List<Vaga> byTitulo = vagaService.findByTituloContainingIgnoreCase(term);
        List<Vaga> byEmpresa = vagaService.findByEmpresaContainingIgnoreCase(term);

        Map<Long, Vaga> merged = new LinkedHashMap<>();
        for (Vaga v : byTitulo) if (v.getId() != null) merged.put(v.getId(), v);
        for (Vaga v : byEmpresa) if (v.getId() != null) merged.put(v.getId(), v);

        List<Vaga> result = merged.values().stream().limit(5).collect(Collectors.toList());
        log.info("searchVagas - resultados={}", result.size());

        return result.stream()
                .map(v -> new VagaDto(
                        v.getId(),
                        v.getTitulo(),
                        v.getEmpresa(),
                        v.getLocal(),
                        shorten(v.getRequisitos(), 400)
                ))
                .collect(Collectors.toList());
    }

    private String extractSearchTerm(String prompt) {
        if (prompt == null) return "";
        String lower = prompt.toLowerCase(Locale.ROOT).trim();

        // 1) Se menciona "vaga(s)", captura o que vem depois
        Pattern p1 = Pattern.compile("\\bvagas?\\b\\s*(?:de|para)?\\s*(.+)$", Pattern.UNICODE_CASE);
        Matcher m1 = p1.matcher(lower);
        if (m1.find()) {
            String cand = normalizeText(m1.group(1));
            if (!cand.isBlank()) return cand;
        }

        // 2) Padrões como "me fale sobre", "fale sobre", "me diga sobre"
        Pattern p2 = Pattern.compile("(?:me\\s+fa[lc]e(?:\\s+sobre)?|fale\\s+sobre|me\\s+diga\\s+sobre|o\\s+que\\s+e|o\\s+que\\s+é)\\s*(?:a|o)?\\s*(?:vaga[s]?\\s*(?:de|para)?\\s*)?(.+)$", Pattern.UNICODE_CASE);
        Matcher m2 = p2.matcher(lower);
        if (m2.find()) {
            String cand = normalizeText(m2.group(1));
            if (!cand.isBlank()) return cand;
        }

        // 3) Remover stopwords comuns e manter o resto
        String cleaned = lower.replaceAll("[^\\p{L}\\p{Nd}\\s]", " ")
                .replaceAll("\\b(vagas?|procuro|busca|buscando|me|sobre|fale|diga|por\\s+favor|porfavor|me\\s+conte)\\b", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();

        return cleaned.isBlank() ? lower.replaceAll("[^\\p{L}\\p{Nd}\\s]", " ").trim() : cleaned;
    }

    private String normalizeText(String s) {
        if (s == null) return "";
        String t = s.replaceAll("[^\\p{L}\\p{Nd}\\s]", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
        return t;
    }

    private String shorten(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}