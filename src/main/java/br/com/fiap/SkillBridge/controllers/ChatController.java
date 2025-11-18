package br.com.fiap.SkillBridge.controllers;

import br.com.fiap.SkillBridge.dto.request.ChatRequest;
import br.com.fiap.SkillBridge.dto.response.ChatResponse;
import br.com.fiap.SkillBridge.services.AIService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Controller responsável pela experiência de chat com IA na SkillBridge.
 *
 * Responsabilidades principais:
 *  - Entregar a página HTML do chat (/chat), já com dados do usuário logado
 *    via OAuth2 (nome / avatar).
 *  - Expor um endpoint HTTP para o frontend enviar perguntas e receber
 *    respostas geradas pela IA (Spring AI via AIService).
 *
 * Observações:
 *  - A autenticação é feita via Spring Security + OAuth2 (ex.: login Google/GitHub).
 *    A regra de "só acessa /chat se estiver autenticado" deve ser configurada
 *    na classe de Security (HttpSecurity).
 *  - A inteligência artificial em si NÃO fica no controller. Toda a lógica
 *    de roteamento (vagas, PDFs, requisitos) e chamada do modelo generativo
 *    está encapsulada no AIService.
 */
@Controller
@RequestMapping("/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final AIService aiService;

    /**
     * Injeção de dependência via construtor.
     * O Spring resolve o AIService automaticamente (bean @Service).
     */
    public ChatController(AIService aiService) {
        this.aiService = aiService;
    }

    // =====================================================================
    // 1. GET /chat → retorna a página de chat (Thymeleaf)
    // =====================================================================

    /**
     * Renderiza a página de chat da SkillBridge.
     *
     * - Se o usuário estiver autenticado via OAuth2, adicionamos ao Model:
     *      • avatar: URL da imagem de perfil (GitHub/Google)
     *      • username: nome amigável a ser mostrado na UI
     * - A view "chat.html" (Thymeleaf) utiliza esses atributos para personalizar
     *   a experiência do usuário (ex: mostrar avatar no header).
     */
    @GetMapping
    public String chatPage(Model model,
                           @AuthenticationPrincipal OAuth2User user) {

        if (user != null) {
            // Tenta pegar a URL do avatar do provedor (ex.: GitHub)
            Object avatarUrl = user.getAttribute("avatar_url");
            if (avatarUrl == null) {
                // Alguns provedores usam outro atributo; aqui você pode adaptar se precisar
                avatarUrl = user.getAttribute("picture");
            }

            // Nome amigável: usa "name" e, se não tiver, cai para "login"
            String username =
                    user.getAttribute("name") != null
                            ? user.getAttribute("name")
                            : user.getAttribute("login");

            model.addAttribute("avatar", avatarUrl);
            model.addAttribute("username", username);
        }

        return "chat"; // Thymeleaf: src/main/resources/templates/chat.html
    }

    // =====================================================================
    // 2. POST /chat/api → endpoint AJAX para conversar com a IA
    // =====================================================================

    /**
     * Endpoint REST simples consumido pelo frontend (AJAX / Fetch).
     *
     * Fluxo:
     *  - O frontend envia um JSON com a mensagem do usuário (ChatRequest).
     *  - Validamos o payload com Bean Validation (@Valid).
     *  - Delegamos a lógica para o AIService.ask(...), que:
     *      • Identifica se é pergunta sobre vagas, PDFs ou ensino.
     *      • Usa Spring AI para gerar resposta a partir de PDFs, quando necessário.
     *  - Devolvemos um ChatResponse com o texto de resposta.
     *
     * @param request objeto com a mensagem do usuário
     * @return ChatResponse com o texto já pronto para exibição
     */
    @PostMapping("/api")
    @ResponseBody
    public ChatResponse chatApi(@RequestBody @Valid ChatRequest request,
                                @AuthenticationPrincipal OAuth2User user) {

        String pergunta = request.getMessage();
        log.info("Requisição de chat recebida de [{}]: {}", // apenas para monitorar
                user != null ? user.getAttribute("email") : "anônimo",
                pergunta);

        String reply = aiService.ask(pergunta);
        return new ChatResponse(reply);
    }
}
