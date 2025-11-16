package br.com.fiap.SkillBridge.services;

import br.com.fiap.SkillBridge.dto.VagaDto;
import br.com.fiap.SkillBridge.tools.VagaTool;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AIService {

    private final PdfLoaderService pdfLoaderService;
    private final VagaTool vagaTool;
    private List<PdfLoaderService.DocumentDto> docs = Collections.emptyList();

    // contexto simples da última busca de vagas (serviço singleton: pode vazar entre usuários)
    private List<VagaDto> lastVagas = Collections.emptyList();

    /*
     \- Prompt orientador: descreve o estilo de resposta desejado.
     \- Responder de forma natural, direta e útil.
     \- Não devolver trechos longos do PDF cru; sintetizar em linguagem humana.
     \- Priorizar contexto de vagas quando a pergunta for sobre vaga; quando pedir para ensinar, explicar requisitos.
    */
    private static final String DEFAULT_SYSTEM_PROMPT =
            "Sistema: responda de forma natural, curta e útil. Quando a pergunta for sobre vagas, liste e explique as vagas encontradas. " +
                    "Quando a pergunta for sobre o documento (PDF), retorne um resumo claro e dirigido à pergunta do usuário — não reproduza o PDF integral. " +
                    "Se o usuário pede para ensinar/explorar requisitos, explique os termos técnicos com exemplos práticos. " +
                    "Se a entrada for um cumprimento curto (ex: 'oi'), voce deve responder cordialmente sem retornar PDF automaticamente.Eu quero que voce so retorne respostas ligadas ao pdf se o usuario mencionar a palavra pdf ou se perguntar sobre o projeto da SkillBridge";

    public AIService(PdfLoaderService pdfLoaderService, VagaTool vagaTool) {
        this.pdfLoaderService = pdfLoaderService;
        this.vagaTool = vagaTool;
    }

    public String ask(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return "Por favor, digite uma pergunta.";
        }

        if (isGreetingQuery(prompt)) {
            return "Olá! Em que posso ajudar? Posso falar sobre vagas cadastradas ou sobre o projeto/documentação.";
        }

        boolean isVaga = isVagaQuery(prompt);
        boolean isPdf = isPdfQuery(prompt);
        boolean isTeach = isTeachQuery(prompt);

        // Removido fallback que ativava PDF por padrão.
        StringBuilder out = new StringBuilder();

        if (isVaga) {
            List<VagaDto> vagas = vagaTool.searchVagas(prompt);

            if (vagas.isEmpty()) {
                // se o usuário pede para ensinar usando referência a resposta anterior, reutiliza lastVagas
                if (isTeach && isReferencingPrevious(prompt) && !lastVagas.isEmpty()) {
                    out.append(explainVagaRequirements(lastVagas));
                    return out.toString().trim();
                }
                out.append("Não encontrei vagas correspondentes.\n");
            } else {
                // atualiza contexto
                lastVagas = vagas;
                if (isTeach) {
                    out.append(explainVagaRequirements(vagas));
                    return out.toString().trim();
                } else {
                    out.append("Encontrei as seguintes vagas:\n");
                    for (VagaDto v : vagas) {
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

    private String answerFromPdfs(String prompt) {
        // usa DEFAULT_SYSTEM_PROMPT como guia para síntese
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
                int idx = -1;
                int from = 0;
                while ((idx = textLower.indexOf(t, from)) >= 0) {
                    score++;
                    from = idx + t.length();
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestDoc = d;
                // extrai snippet em torno do token mais relevante (o primeiro token)
                bestSnippet = extractSnippet(d.getText(), tokens.get(0));
            }
        }

        if (bestDoc != null && bestScore > 0 && bestSnippet != null) {
            String cleaned = cleanSnippet(bestSnippet);
            // sintetiza em linguagem natural, priorizando perguntas curtas e direta
            String summary = synthesizeForUser(cleaned, prompt);
            String greeting = chooseGreeting(prompt);
            return greeting + " Sobre o projeto (trecho de `" + bestDoc.getName() + "`):\n\n" + summary;
        }

        return "";
    }

    private boolean isVagaQuery(String prompt) {
        if (prompt == null) return false;
        String p = prompt.toLowerCase(Locale.ROOT);
        return p.matches(".*\\b(vaga|vagas|emprego|empregos|oportunidade|oportunidades|contrata|contrata-se|empresa|t[ií]tulo|titulo|analista|desenvolvedor|pleno|junior|sênior|sr\\.|jr\\.)\\b.*");
    }

    private boolean isPdfQuery(String prompt) {
        if (prompt == null) return false;
        String p = prompt.toLowerCase(Locale.ROOT);
        // Agora considera PDF apenas se o usuário mencionar explicitamente 'pdf' ou 'skillbridge' (ou 'skill bridge')
        if (p.contains("pdf")) return true;
        if (p.contains("skillbridge") || p.contains("skill bridge")) return true;
        // também aceita formas como "projeto da skillbridge" ou "projeto skillbridge"
        if (p.matches(".*\\bprojeto\\s+da\\s+skillbridge\\b.*") || p.matches(".*\\bprojeto\\s+skillbridge\\b.*")) return true;
        return false;
    }

    private boolean isTeachQuery(String prompt) {
        if (prompt == null) return false;
        String p = prompt.toLowerCase(Locale.ROOT);
        return p.matches(".*\\b(ensine|me ensine|explique|me explique|como|aprenda|ensina|me ensina|me explique|o que são|o que é)\\b.*")
                || p.contains("requisitos") || p.contains("me ensine sobre") || p.contains("me ensina sobre");
    }

    private boolean isGreetingQuery(String prompt) {
        if (prompt == null) return false;
        String p = prompt.toLowerCase(Locale.ROOT).trim();
        return p.matches("^(oi|ol[aá]|ola|bom dia|boa tarde|boa noite|e ai|ei)([\\.!\\?\\s].*)?$");
    }

    // detecta se o prompt referencia algo anterior (ex: "me explique esses requisitos")
    private boolean isReferencingPrevious(String prompt) {
        if (prompt == null) return false;
        String p = prompt.toLowerCase(Locale.ROOT);
        return p.matches(".*\\b(esse|essa|esses|essas|estes|aqueles|isso|aquilo|eles|os requisitos|esses requisitos|me explique esses|explique esses|explique os requisitos|me explique os requisitos)\\b.*");
    }

    private String explainVagaRequirements(List<VagaDto> vagas) {
        if (vagas == null || vagas.isEmpty()) return "Nenhuma vaga para extrair requisitos.";
        Set<String> tokens = new LinkedHashSet<>();
        for (VagaDto v : vagas) {
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

    private String synthesizeForUser(String text, String prompt) {
        if (text == null || text.isEmpty()) return "";
        // tenta extrair 2-3 frases iniciais relevantes e reescrever de forma direta
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
        // se muito curto, usar fallback truncado
        if (result.length() < 30) {
            result = text.length() > 400 ? text.substring(0, 400) + "..." : text;
        }
        // adicionar nota de origem para transparência
        result += "\n\n(Resumo gerado a partir do documento, ajustado para linguagem natural.)";
        return result;
    }

    private String chooseGreeting(String prompt) {
        String[] greetings = {
                "Olá, aqui vai um resumo.",
                "Segue uma explicação:",
                "Resumo encontrado:",
                "Posso ajudar com isso — veja abaixo:"
        };
        int idx = Math.abs(Objects.hashCode(prompt)) % greetings.length;
        return greetings[idx] + " ";
    }

    private String cleanSnippet(String text) {
        if (text == null) return "";
        String t = text.replaceAll("■", " ").replaceAll("\\s{2,}", " ").trim();
        return t.length() <= 1500 ? t : t.substring(0, 1500) + "...";
    }

    private String extractSnippet(String text, String token) {
        if (text == null || token == null) return "";
        String lower = text.toLowerCase(Locale.ROOT);
        int idx = lower.indexOf(token.toLowerCase(Locale.ROOT));
        if (idx < 0) return text.length() <= 500 ? text : text.substring(0, 500);
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
}