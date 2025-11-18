package br.com.fiap.SkillBridge.tools;

import br.com.fiap.SkillBridge.dto.response.VagaResponse;
import br.com.fiap.SkillBridge.models.Vaga;
import br.com.fiap.SkillBridge.services.VagaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Componente utilitário para busca inteligente de vagas.
 *
 * Responsabilidades:
 *  - Interpretar a pergunta em linguagem natural do usuário
 *    (ex: "me mostre vagas de Java", "fale sobre vagas para analista")
 *  - Extrair um termo de busca relevante a partir da frase
 *  - Consultar o VagaService por título e empresa
 *  - Mesclar resultados e devolver uma lista reduzida (top 5)
 *
 * Este componente é usado principalmente pelo AIService
 * para enriquecer respostas do chat com vagas reais do sistema.
 */
@Component
public class VagaTool {

    private static final Logger log = LoggerFactory.getLogger(VagaTool.class);

    private final VagaService vagaService;

    public VagaTool(VagaService vagaService) {
        this.vagaService = vagaService;
    }

    /**
     * Faz uma busca "semântica simples" de vagas com base em um texto
     * livre digitado pelo usuário.
     *
     * Fluxo:
     *  1) Extrai um termo de busca usando expressões regulares
     *  2) Consulta vagas por título e por empresa
     *  3) Mescla resultados, removendo duplicados
     *  4) Limita a quantidade a 5 itens
     *  5) Converte para DTO de resposta (VagaResponse), encurtando requisitos
     *
     * @param query texto digitado pelo usuário (ex: "me mostre vagas de Java remoto")
     * @return lista de VagaResponse para uso no chat/IA
     */
    public List<VagaResponse> searchVagas(String query) {
        String term = extractSearchTerm(query);
        log.info("searchVagas - query='{}' -> term='{}'", query, term);

        if (term == null || term.isBlank()) {
            log.info("searchVagas - termo vazio após normalização");
            return Collections.emptyList();
        }

        // Busca por título e por empresa separadamente
        List<Vaga> byTitulo = vagaService.findByTituloContainingIgnoreCase(term);
        List<Vaga> byEmpresa = vagaService.findByEmpresaContainingIgnoreCase(term);

        // Mescla removendo duplicados (key = id)
        Map<Long, Vaga> merged = new LinkedHashMap<>();
        for (Vaga v : byTitulo) {
            if (v.getId() != null) {
                merged.put(v.getId(), v);
            }
        }
        for (Vaga v : byEmpresa) {
            if (v.getId() != null) {
                merged.put(v.getId(), v);
            }
        }

        List<Vaga> result = merged.values()
                .stream()
                .limit(5) // limita para evitar resposta gigante no chat
                .collect(Collectors.toList());

        log.info("searchVagas - resultados={}", result.size());

        // Converte entidade -> DTO de saída (VagaResponse) com requisitos encurtados
        return result.stream()
                .map(v -> new VagaResponse(
                        v.getId(),
                        v.getTitulo(),
                        v.getEmpresa(),
                        v.getLocal(),
                        shorten(v.getRequisitos(), 400)
                ))
                .collect(Collectors.toList());
    }

    /**
     * Extrai um termo de busca da frase completa do usuário.
     *
     * Exemplo:
     *  - "me mostre vagas de Java"       -> "java"
     *  - "fale sobre vagas para pleno"   -> "pleno"
     *  - "procuro vaga desenvolvedor"    -> "desenvolvedor"
     *
     * Caso não encaixe em nenhum padrão, faz uma limpeza básica
     * removendo stopwords simples.
     */
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

        // 2) Padrões como "me fale sobre", "fale sobre", "me diga sobre", "o que é"
        Pattern p2 = Pattern.compile(
                "(?:me\\s+fa[lc]e(?:\\s+sobre)?|fale\\s+sobre|me\\s+diga\\s+sobre|o\\s+que\\s+e|o\\s+que\\s+é)\\s*" +
                        "(?:a|o)?\\s*(?:vaga[s]?\\s*(?:de|para)?\\s*)?(.+)$",
                Pattern.UNICODE_CASE
        );
        Matcher m2 = p2.matcher(lower);
        if (m2.find()) {
            String cand = normalizeText(m2.group(1));
            if (!cand.isBlank()) return cand;
        }

        // 3) Remover stopwords comuns e manter o resto como termo de busca
        String cleaned = lower
                .replaceAll("[^\\p{L}\\p{Nd}\\s]", " ")
                .replaceAll("\\b(vagas?|procuro|busca|buscando|me|sobre|fale|diga|por\\s+favor|porfavor|me\\s+conte)\\b", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();

        // Se a limpeza ficou vazia, ao menos devolve o texto sem pontuação
        return cleaned.isBlank()
                ? lower.replaceAll("[^\\p{L}\\p{Nd}\\s]", " ").trim()
                : cleaned;
    }

    /**
     * Normaliza texto simples removendo caracteres estranhos e espaços duplicados.
     */
    private String normalizeText(String s) {
        if (s == null) return "";
        return s.replaceAll("[^\\p{L}\\p{Nd}\\s]", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    /**
     * Encurta um texto para no máximo {@code max} caracteres.
     * Usado principalmente para cortar a descrição de requisitos
     * nas respostas do chat.
     */
    private String shorten(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}
