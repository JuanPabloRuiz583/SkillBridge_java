package br.com.fiap.SkillBridge.services;

import br.com.fiap.SkillBridge.dto.response.VagaResponse;
import br.com.fiap.SkillBridge.tools.VagaTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço de orquestração de "IA" da SkillBridge.
 *
 * Responsabilidades principais:
 *  - Roteia a pergunta do usuário entre:
 *      • consulta de vagas (via VagaTool)
 *      • análise de PDFs do projeto SkillBridge
 *      • explicação de requisitos de vagas
 *  - Utiliza Spring AI (ChatClient) para gerar respostas em linguagem natural
 *    a partir de trechos dos PDFs (IA generativa).
 *
 * Observações importantes:
 *  - Este serviço é @Service singleton; alguns estados simples (lastVagas, docs)
 *    podem ser compartilhados entre usuários. Para produção, considere escopo
 *    por sessão/usuário ou contexto separado.
 */
@Service
public class AIService {

    // =========================================================================
    // 1. Dependências e estado interno
    // =========================================================================

    private static final Logger log = LoggerFactory.getLogger(AIService.class);

    private final PdfLoaderService pdfLoaderService;
    private final VagaTool vagaTool;

    /**
     * Cliente de IA generativa provido pelo Spring AI.
     * É construído a partir do ChatClient.Builder configurado pelo starter
     * (spring-ai-openai-spring-boot-starter).
     */
    private final ChatClient chatClient;

    /**
     * Cache simples em memória dos documentos carregados do classpath.
     * Cada DocumentDto contém nome + texto extraído do PDF.
     */
    private List<PdfLoaderService.DocumentDto> docs = Collections.emptyList();

    /**
     * Contexto da última busca de vagas.
     * Agora usando o DTO de resposta (VagaResponse), que é o que
     * a camada de apresentação/REST deveria enxergar.
     *
     * Usado para responder perguntas do tipo:
     * "me explique esses requisitos" logo após listar vagas.
     */
    private List<VagaResponse> lastVagas = Collections.emptyList();

    /*
     * Prompt orientador para o modelo de IA:
     *  - Responder de forma natural, direta e útil.
     *  - Não devolver trechos longos do PDF cru; sintetizar em linguagem humana.
     *  - Priorizar contexto de vagas quando a pergunta for sobre vaga; quando
     *    pedir para ensinar, explicar requisitos com exemplos práticos.
     *  - Só usar PDF quando o usuário mencionar PDF ou o projeto SkillBridge.
     */
    private static final String DEFAULT_SYSTEM_PROMPT =
            "Sistema: responda de forma natural, curta e útil. Quando a pergunta for sobre vagas, liste e explique as vagas encontradas. " +
                    "Quando a pergunta for sobre o documento (PDF), retorne um resumo claro e dirigido à pergunta do usuário — não reproduza o PDF integral. " +
                    "Se o usuário pede para ensinar/explorar requisitos, explique os termos técnicos com exemplos práticos. " +
                    "Se a entrada for um cumprimento curto (ex: 'oi'), você deve responder cordialmente sem retornar PDF automaticamente. " +
                    "Você só deve usar o conteúdo dos PDFs quando o usuário mencionar explicitamente a palavra 'pdf' ou fizer perguntas sobre o projeto SkillBridge.";

    // =========================================================================
    // 2. Construtor com injeção de dependências
    // =========================================================================

    public AIService(
            PdfLoaderService pdfLoaderService,
            VagaTool vagaTool,
            ChatClient.Builder chatClientBuilder // vindo do Spring AI
    ) {
        this.pdfLoaderService = pdfLoaderService;
        this.vagaTool = vagaTool;
        this.chatClient = chatClientBuilder.build();
    }

    // =========================================================================
    // 3. Operação pública: entrada única do chat
    // =========================================================================

    /**
     * Ponto de entrada principal do "chat" da SkillBridge.
     *
     * @param prompt mensagem bruta digitada pelo usuário
     * @return texto de resposta já pronto para exibição no frontend
     */
    public String ask(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return "Por favor, digite uma pergunta.";
        }

        if (isGreetingQuery(prompt)) {
            return "Olá! Em que posso ajudar? Posso falar sobre vagas cadastradas ou sobre o projeto/documentação (PDFs da SkillBridge).";
        }

        boolean isVaga = isVagaQuery(prompt);
        boolean isPdf = isPdfQuery(prompt);
        boolean isTeach = isTeachQuery(prompt);

        StringBuilder out = new StringBuilder();

        // ------------------------------------------------------------
        // 3.1. Roteio para consultas de vagas / requisitos
        // ------------------------------------------------------------
        if (isVaga) {
            // ⬇️ Agora usamos VagaResponse como DTO de saída
            List<VagaResponse> vagas = vagaTool.searchVagas(prompt);

            if (vagas.isEmpty()) {
                // Se o usuário pede para ensinar usando referência à resposta anterior
                if (isTeach && isReferencingPrevious(prompt) && !lastVagas.isEmpty()) {
                    out.append(explainVagaRequirements(lastVagas));
                    return out.toString().trim();
                }
                out.append("Não encontrei vagas correspondentes.\n");
            } else {
                // Atualiza contexto de vagas
                lastVagas = vagas;

                if (isTeach) {
                    out.append(explainVagaRequirements(vagas));
                    return out.toString().trim();
                } else {
                    out.append("Encontrei as seguintes vagas:\n");
                    for (VagaResponse v : vagas) {
                        out.append("- ").append(nullToEmpty(v.getTitulo()));
                        if (v.getEmpresa() != null && !v.getEmpresa().isBlank()) {
                            out.append(" — ").append(v.getEmpresa());
                        }
                        if (v.getLocal() != null && !v.getLocal().isBlank()) {
                            out.append(" (").append(v.getLocal()).append(")");
                        }
                        out.append("\n  Requisitos: ").append(nullToEmpty(v.getRequisitos())).append("\n");
                    }
                }
            }
        } else {
            // Se não for consulta de vaga, mas for ensino referenciando anterior, tenta reutilizar lastVagas
            if (isTeach && isReferencingPrevious(prompt) && !lastVagas.isEmpty()) {
                out.append(explainVagaRequirements(lastVagas));
                return out.toString().trim();
            }
        }

        // ------------------------------------------------------------
        // 3.2. Roteio para PDFs (documentação SkillBridge)
        // ------------------------------------------------------------
        if (isPdf) {
            if (docs.isEmpty()) {
                docs = pdfLoaderService.loadAllFromClasspathDoc();
            }
            if (docs.isEmpty()) {
                if (out.length() > 0) out.append("\n");
                out.append("Nenhum PDF lido. Verifique se os arquivos estão em `src/main/resources/doc`.");
            } else {
                String pdfAnswer = answerFromPdfs(prompt);
                if (!pdfAnswer.isBlank()) {
                    if (out.length() > 0) out.append("\n\n");
                    out.append(pdfAnswer);
                } else {
                    if (out.length() == 0) {
                        out.append("Não encontrei resposta específica nos PDFs. Tente reformular a pergunta.");
                    }
                }
            }
        }

        return out.toString().trim();
    }

    // =========================================================================
    // 4. Lógica de busca/resumo em PDFs + IA generativa
    // =========================================================================

    private String answerFromPdfs(String prompt) {
        String contextualPrompt = DEFAULT_SYSTEM_PROMPT + "\n\nPergunta: " + prompt;
        String normalized = contextualPrompt.toLowerCase(Locale.ROOT);

        List<String> tokens = Arrays.stream(normalized.split("\\W+"))
                .filter(s -> s.length() > 0)
                .collect(Collectors.toList());
        if (tokens.isEmpty()) return "Pergunta inválida.";

        PdfLoaderService.DocumentDto bestDoc = null;
        int bestScore = 0;
        String bestSnippet = null;

        for (PdfLoaderService.DocumentDto d : docs) {
            String textLower = d.getText().toLowerCase(Locale.ROOT);
            int score = 0;
            for (String t : tokens) {
                int idx;
                int from = 0;
                while ((idx = textLower.indexOf(t, from)) >= 0) {
                    score++;
                    from = idx + t.length();
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestDoc = d;
                bestSnippet = extractSnippet(d.getText(), tokens.get(0));
            }
        }

        if (bestDoc != null && bestScore > 0 && bestSnippet != null) {
            String cleaned = cleanSnippet(bestSnippet);
            String summary = synthesizeForUser(cleaned, prompt);
            String greeting = chooseGreeting(prompt);
            return greeting + " Sobre o projeto (trecho de `" + bestDoc.getName() + "`):\n\n" + summary;
        }

        return "";
    }

    private String synthesizeForUser(String text, String prompt) {
        if (text == null || text.isEmpty()) return "";

        String localSummary = localSummarize(text);

        try {
            String response = chatClient
                    .prompt()
                    .system(DEFAULT_SYSTEM_PROMPT)
                    .user(
                            "Pergunta do usuário: " + prompt + "\n\n" +
                                    "Abaixo está um trecho do documento (PDF) relacionado:\n" +
                                    "------------------------------\n" +
                                    text + "\n" +
                                    "------------------------------\n\n" +
                                    "Com base SOMENTE nesse contexto, gere uma resposta em português do Brasil, " +
                                    "bem objetiva, em no máximo 2 parágrafos, explicando de forma clara para o usuário."
                    )
                    .call()
                    .content();

            if (response == null || response.isBlank()) {
                return localSummary;
            }
            return response.trim() + "\n\n(Resposta gerada com IA a partir do documento da SkillBridge.)";
        } catch (Exception ex) {
            log.warn("Falha ao chamar Spring AI para síntese de PDF. Usando fallback local.", ex);
            return localSummary;
        }
    }

    private String localSummarize(String text) {
        String[] parts = text.split("(?<=[\\.\\!\\?])\\s+");
        StringBuilder sb = new StringBuilder();
        sb.append("Resposta breve: ");
        int taken = 0;
        for (String p : parts) {
            String s = p.trim();
            if (!s.isEmpty()) {
                sb.append(s);
                taken++;
                if (taken >= 2) break;
                sb.append(" ");
            }
        }
        String result = sb.toString().trim();
        if (result.length() < 30) {
            result = text.length() > 400 ? text.substring(0, 400) + "..." : text;
        }
        result += "\n\n(Resumo local gerado a partir do documento, sem IA externa.)";
        return result;
    }

    // =========================================================================
    // 5. Heurísticas de roteamento semântico (vaga/pdf/ensino/saudação)
    // =========================================================================

    private boolean isVagaQuery(String prompt) {
        if (prompt == null) return false;
        String p = prompt.toLowerCase(Locale.ROOT);
        return p.matches(".*\\b(vaga|vagas|emprego|empregos|oportunidade|oportunidades|contrata|contrata-se|empresa|t[ií]tulo|titulo|analista|desenvolvedor|pleno|junior|sênior|sr\\.|jr\\.)\\b.*");
    }

    private boolean isPdfQuery(String prompt) {
        if (prompt == null) return false;
        String p = prompt.toLowerCase(Locale.ROOT);
        if (p.contains("pdf")) return true;
        if (p.contains("skillbridge") || p.contains("skill bridge")) return true;
        if (p.matches(".*\\bprojeto\\s+da\\s+skillbridge\\b.*") || p.matches(".*\\bprojeto\\s+skillbridge\\b.*")) return true;
        return false;
    }

    private boolean isTeachQuery(String prompt) {
        if (prompt == null) return false;
        String p = prompt.toLowerCase(Locale.ROOT);
        return p.matches(".*\\b(ensine|me ensine|explique|me explique|como|aprenda|ensina|me ensina|o que são|o que é)\\b.*")
                || p.contains("requisitos")
                || p.contains("me ensine sobre")
                || p.contains("me ensina sobre");
    }

    private boolean isGreetingQuery(String prompt) {
        if (prompt == null) return false;
        String p = prompt.toLowerCase(Locale.ROOT).trim();
        return p.matches("^(oi|ol[aá]|ola|bom dia|boa tarde|boa noite|e ai|ei)([\\.!\\?\\s].*)?$");
    }

    private boolean isReferencingPrevious(String prompt) {
        if (prompt == null) return false;
        String p = prompt.toLowerCase(Locale.ROOT);
        return p.matches(".*\\b(esse|essa|esses|essas|estes|aqueles|isso|aquilo|eles|os requisitos|esses requisitos|me explique esses|explique esses|explique os requisitos|me explique os requisitos)\\b.*");
    }

    // =========================================================================
    // 6. Auxiliares para requisitos de vagas (agora com VagaResponse)
    // =========================================================================

    private String explainVagaRequirements(List<VagaResponse> vagas) {
        if (vagas == null || vagas.isEmpty()) return "Nenhuma vaga para extrair requisitos.";
        Set<String> tokens = new LinkedHashSet<>();
        for (VagaResponse v : vagas) {
            String req = v.getRequisitos();
            if (req != null && !req.isBlank()) {
                for (String part : req.split("[,;]")) {
                    String t = part.trim().toLowerCase(Locale.ROOT);
                    if (!t.isEmpty()) tokens.add(t);
                }
            }
        }

        if (tokens.isEmpty()) return "As vagas não possuem requisitos detalhados.";

        StringBuilder sb = new StringBuilder();
        sb.append("Posso explicar os principais requisitos encontrados nas vagas:\n\n");
        for (String t : tokens) {
            sb.append("- ").append(capitalizeFirst(t)).append(": ").append(getShortExplanationForTerm(t)).append("\n");
        }
        sb.append("\nDiga se quer exemplos práticos, exercícios ou links de estudo.");
        return sb.toString();
    }

    private String getShortExplanationForTerm(String term) {
        if (term.contains("java")) {
            return "Java 11+ — versão LTS; foco em features modernas (var, streams, API de Date/Time), boas práticas OOP, gerenciamento de dependências com Maven/Gradle e entendimento de JVM.";
        }
        if (term.contains("spring boot") || term.contains("springboot") || term.contains("spring")) {
            return "Spring Boot — framework para criar aplicações Java rapidamente; entenda Injeção de Dependência, controllers, Spring Data JPA, profiles e configuração automática.";
        }
        if (term.contains("rest") || term.contains("api") || term.contains("restful")) {
            return "REST — design de APIs HTTP: endpoints (GET/POST/PUT/DELETE), códigos de status, JSON, autenticação/autorização e documentação (OpenAPI/Swagger).";
        }
        if (term.contains("sql") || term.contains("database") || term.contains("banco")) {
            return "SQL — consultas relacionais (SELECT, JOIN, GROUP BY), índices, transações e acesso via JDBC/Spring Data; importante para performance e integridade dos dados.";
        }
        if (term.contains("python")) {
            return "Python — linguagem usada em análise de dados; prática com bibliotecas como pandas e scripts para ETL.";
        }
        if (term.contains("power bi") || term.contains("powerbi")) {
            return "Power BI — ferramenta de visualização; criar dashboards, relatórios e conectar a fontes de dados.";
        }
        // fallback genérico
        return "Conceito comum na vaga — consulte a documentação oficial e pratique com pequenos projetos.";
    }

    // =========================================================================
    // 7. Utilitários de texto
    // =========================================================================

    private String cleanSnippet(String text) {
        if (text == null) return "";
        String t = text.replaceAll("■", " ").replaceAll("\\s{2,}", " ").trim();
        return t.length() <= 1500 ? t : t.substring(0, 1500) + "...";
    }

    private String extractSnippet(String text, String token) {
        if (text == null || token == null) return "";
        String lower = text.toLowerCase(Locale.ROOT);
        int idx = lower.indexOf(token.toLowerCase(Locale.ROOT));
        if (idx < 0) return text.length() <= 500 ? text.substring(0, 500) : text.substring(0, 500);
        int start = Math.max(0, idx - 200);
        int end = Math.min(text.length(), idx + 300);
        return text.substring(start, end);
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private String capitalizeFirst(String s) {
        if (s == null || s.isBlank()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // -------------------------------------------------------------------------
    // 7.1 Saudação dinâmica para respostas (método que estava faltando)
    // -------------------------------------------------------------------------

    /**
     * Gera uma saudação curta baseada no conteúdo do prompt,
     * só para deixar a resposta mais "humana".
     */
    private String chooseGreeting(String prompt) {
        String[] greetings = {
                "Olá, aqui vai um resumo.",
                "Segue uma explicação:",
                "Resumo encontrado:",
                "Posso ajudar com isso — veja abaixo:"
        };

        int idx = 0;
        if (prompt != null && !prompt.isBlank()) {
            idx = Math.abs(Objects.hashCode(prompt)) % greetings.length;
        }
        return greetings[idx] + " ";
    }
}
