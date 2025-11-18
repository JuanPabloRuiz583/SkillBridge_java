// java
package br.com.fiap.SkillBridge.controllers;

import br.com.fiap.SkillBridge.models.Vaga;
import br.com.fiap.SkillBridge.services.VagaService;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;

/**
 * Controller MVC responsável por gerenciar o fluxo de vagas na aplicação.
 *
 * Funcionalidades:
 *  - Listar vagas para o usuário logado (index)
 *  - Exibir formulário de criação/edição (form)
 *  - Criar nova vaga (POST /vaga/form)
 *  - Editar vaga existente (POST /vaga/edit/{id})
 *  - Excluir vaga (POST /vaga/delete/{id})
 *  - Pesquisar vagas por empresa ou título (/vaga/search)
 *
 * Observações:
 *  - Integra com autenticação via OAuth2 (GitHub/Google).
 *  - Usa MessageSource para internacionalização das mensagens (i18n).
 *  - Usa Bean Validation (@Valid) para validar a entidade Vaga.
 *
 * Futuro:
 *  - Migrar o binding do formulário para um VagaRequestDto
 *    e retornar dados via VagaResponseDto em APIs REST.
 */
@Controller
@RequestMapping("/vaga")
public class VagaController {

    private final VagaService vagaService;
    private final MessageSource messageSource;

    public VagaController(VagaService vagaService, MessageSource messageSource) {
        this.vagaService = vagaService;
        this.messageSource = messageSource;
    }

    // =========================================================
    // 1) LISTAGEM PRINCIPAL DE VAGAS (HOME LOGADA)
    // =========================================================
    @GetMapping
    public String index(Model model, @AuthenticationPrincipal OAuth2User user) {
        if (user == null) {
            // Se não estiver autenticado, redireciona para tela de login
            return "redirect:/login";
        }

        List<Vaga> vagas = vagaService.getAllVagas();
        model.addAttribute("vagas", vagas);
        model.addAttribute("user", user);

        // Avatar pode vir de "picture" (Google) ou "avatar_url" (GitHub)
        Object avatar = user.getAttribute("picture") != null
                ? user.getAttribute("picture")
                : user.getAttribute("avatar_url");
        model.addAttribute("avatar", avatar);

        return "index"; // Thymeleaf template: src/main/resources/templates/index.html
    }

    // =========================================================
    // 2) FORMULÁRIO DE CRIAÇÃO / EDIÇÃO (GET)
    // =========================================================
    @GetMapping("/form")
    public String form(Model model, Vaga vaga) {
        // Se já vier um objeto "vaga" (edição), ele é reaproveitado;
        // senão, o Spring instancia automaticamente um novo.
        model.addAttribute("vaga", vaga);
        return "form"; // template: form.html
    }

    // =========================================================
    // 3) CRIAÇÃO DE NOVA VAGA (POST)
    // =========================================================
    @PostMapping("/form")
    public String saveVaga(@Valid @ModelAttribute("vaga") Vaga vaga,
                           BindingResult result,
                           Model model,
                           RedirectAttributes redirectAttributes) {

        // Validações de Bean Validation (@NotBlank, etc.)
        if (result.hasErrors()) {
            // Retorna para o formulário exibindo mensagens de erro
            return "form";
        }

        try {
            vagaService.save(vaga);

            // Mensagem internacionalizada: "vaga.created.success"
            String successMessage = getMessage(
                    "vaga.created.success",
                    null,
                    "Vaga criada com sucesso."
            );
            redirectAttributes.addFlashAttribute("message", successMessage);

        } catch (DataIntegrityViolationException e) {
            // Erro de integridade (ex: campo único duplicado)
            String errorMessage = getMessage(
                    "vaga.save.error",
                    null,
                    e.getMessage() != null ? e.getMessage() : "Erro ao salvar vaga."
            );
            result.reject("error.vaga", errorMessage);
            return "form";
        }

        return "redirect:/vaga";
    }

    // =========================================================
    // 4) EXCLUSÃO DE VAGA (POST)
    // =========================================================
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id,
                         RedirectAttributes redirectAttributes) {

        vagaService.deleteById(id);

        String message = getMessage(
                "vaga.deleted.success",
                null,
                "Vaga excluída com sucesso."
        );
        redirectAttributes.addFlashAttribute("message", message);

        return "redirect:/vaga";
    }

    // =========================================================
    // 5) FORMULÁRIO DE EDIÇÃO (GET)
    // =========================================================
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Vaga vaga = vagaService.findById(id);
        model.addAttribute("vaga", vaga);
        return "form";
    }

    // =========================================================
    // 6) ATUALIZAÇÃO DE VAGA EXISTENTE (POST)
    // =========================================================
    @PostMapping("/edit/{id}")
    public String updateVaga(@PathVariable Long id,
                             @Valid @ModelAttribute("vaga") Vaga vagaAtualizada,
                             BindingResult result,
                             Model model,
                             RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("vaga", vagaAtualizada);
            return "form";
        }

        Vaga vagaExistente = vagaService.findById(id);
        vagaExistente.setTitulo(vagaAtualizada.getTitulo());
        vagaExistente.setRequisitos(vagaAtualizada.getRequisitos());
        vagaExistente.setEmpresa(vagaAtualizada.getEmpresa());
        vagaExistente.setLocal(vagaAtualizada.getLocal());

        vagaService.update(id, vagaExistente);

        String message = getMessage(
                "vaga.updated.success",
                null,
                "Vaga atualizada com sucesso."
        );
        redirectAttributes.addFlashAttribute("message", message);

        return "redirect:/vaga";
    }

    // =========================================================
    // 7) PESQUISA DE VAGAS (POR EMPRESA OU TÍTULO)
    // =========================================================
    @GetMapping("/search")
    public String search(@RequestParam(required = false) String empresa,
                         @RequestParam(required = false) String titulo,
                         Model model,
                         @AuthenticationPrincipal OAuth2User user) {

        if (user == null) {
            return "redirect:/login";
        }

        List<Vaga> vagas;

        if (empresa != null && !empresa.isBlank()) {
            vagas = vagaService.findByEmpresaContainingIgnoreCase(empresa);
        } else if (titulo != null && !titulo.isBlank()) {
            vagas = vagaService.findByTituloContainingIgnoreCase(titulo);
        } else {
            vagas = vagaService.getAllVagas();
        }

        model.addAttribute("vagas", vagas);
        model.addAttribute("user", user);

        Object avatar = user.getAttribute("picture") != null
                ? user.getAttribute("picture")
                : user.getAttribute("avatar_url");
        model.addAttribute("avatar", avatar);

        return "index";
    }

    // =========================================================
    // MÉTODO AUXILIAR → I18N
    // =========================================================
    private String getMessage(String code, Object[] args, String defaultMessage) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, args, defaultMessage, locale);
    }
}
