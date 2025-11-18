package br.com.fiap.SkillBridge.auth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller responsável pelas telas de autenticação.
 *
 * Integração com Spring Security:
 *  - A URL {@code /login} é usada como página de login customizada
 *    (configurada em HttpSecurity.loginPage("/login")).
 *  - O logout "de verdade" é tratado pelo filtro do Spring Security
 *    (normalmente via POST /logout). Após o logout, você pode configurar
 *    o {@code logoutSuccessUrl("/logout")} para exibir a view de saída.
 *
 * Views esperadas (Thymeleaf em src/main/resources/templates):
 *  - login.html
 *  - logout.html
 */
@Controller
public class AuthController {

    /**
     * Exibe a página de login customizada.
     *
     * Essa rota deve estar alinhada com a configuração do Spring Security:
     *   http
     *     .formLogin(form -> form
     *         .loginPage("/login")
     *         ...
     *     );
     *
     * @return nome da view de login (login.html)
     */
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    /**
     * Exibe a página pós-logout (mensagem de "sessão encerrada", etc.).
     *
     * O logout em si é feito pelo Spring Security.
     * Basta configurar em HttpSecurity algo como:
     *
     *   http
     *     .logout(logout -> logout
     *         .logoutUrl("/logout")
     *         .logoutSuccessUrl("/logout") // redireciona para esta view
     *     );
     *
     * @return nome da view de logout (logout.html)
     */
    @GetMapping("/logout")
    public String logoutPage() {
        return "logout";
    }
}
