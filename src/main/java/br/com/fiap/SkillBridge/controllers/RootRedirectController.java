package br.com.fiap.SkillBridge;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * ============================================================
 *  CONTROLLER: RootRedirectController
 * ============================================================
 * Responsável por tratar acessos à raiz da aplicação ("/")
 * e redirecionar automaticamente para a tela de login.
 *
 * Comportamento:
 * - Ao acessar http://localhost:8080/
 *   o usuário será redirecionado (HTTP 302) para /login.
 *
 * Observação:
 * - Esse redirect assume que existe uma rota /login mapeada,
 *   seja por:
 *   - Spring MVC + Thymeleaf, ou
 *   - Spring Security com formLogin(), ou
 *   - algum controller/view responsável por /login.
 */
@Controller
public class RootRedirectController {

    /**
     * Mapeia a URL raiz ("/") e redireciona para "/login".
     *
     * @return String de redirect entendida pelo Spring MVC.
     */
    @GetMapping("/")
    public String redirectToLogin() {
        return "redirect:/login";
    }
}
