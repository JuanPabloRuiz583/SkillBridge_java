package br.com.fiap.SkillBridge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração central de segurança da aplicação.
 *
 * - Autenticação via OAuth2 Login (GitHub / Google)
 * - Controle de acesso às rotas
 * - Tratamento de logout
 * - Exceção de CSRF para o endpoint de chat (/chat/api),
 *   que é chamado via AJAX pelo frontend.
 */
@Configuration
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // ------------------------------------------------------------
                // 1) Autorização de requests
                // ------------------------------------------------------------
                .authorizeHttpRequests(auth -> auth
                        // Páginas públicas
                        .requestMatchers(
                                "/login",
                                "/error"
                        ).permitAll()

                        // Recursos estáticos (CSS, JS, imagens, etc.)
                        .requestMatchers(
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/webjars/**"
                        ).permitAll()

                        // Qualquer outra rota exige usuário autenticado
                        .anyRequest().authenticated()
                )

                // ------------------------------------------------------------
                // 2) Configuração do OAuth2 Login
                // ------------------------------------------------------------
                .oauth2Login(oauth2 -> oauth2
                        // Página customizada de login (Thymeleaf: templates/login.html)
                        .loginPage("/login")
                        // Para onde redirecionar depois de logar com sucesso
                        .defaultSuccessUrl("/vaga", true)
                        .permitAll()
                )

                // ------------------------------------------------------------
                // 3) Logout
                // ------------------------------------------------------------
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        // Redireciona para a tela de login com parâmetro de sucesso
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                )

                // ------------------------------------------------------------
                // 4) CSRF
                // ------------------------------------------------------------
                // Mantém CSRF para formulários Thymeleaf,
                // mas ignora para o endpoint AJAX do chat.
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/chat/api")
                );

        return http.build();
    }
}
