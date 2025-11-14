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

@Controller
@RequestMapping("/vaga")
public class VagaController {

    private final VagaService vagaService;
    private final MessageSource messageSource;

    public VagaController(VagaService vagaService, MessageSource messageSource) {
        this.vagaService = vagaService;
        this.messageSource = messageSource;
    }

    @GetMapping
    public String index(Model model, @AuthenticationPrincipal OAuth2User user) {
        if (user == null) {
            return "redirect:/login";
        }
        List<Vaga> vagas = vagaService.getAllVagas();
        model.addAttribute("vagas", vagas);
        model.addAttribute("user", user);
        var avatar = user.getAttribute("picture") != null ? user.getAttribute("picture") : user.getAttribute("avatar_url");
        model.addAttribute("avatar", avatar);
        return "index";
    }

    @GetMapping("/form")
    public String form(Model model, Vaga vaga) {
        model.addAttribute("vaga", vaga);
        return "form";
    }

    @PostMapping("/form")
    public String saveVaga(@Valid @ModelAttribute Vaga vaga,
                           BindingResult result,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "form";
        }
        try {
            vagaService.save(vaga);
            redirectAttributes.addFlashAttribute("message", "Vaga criada com sucesso");
        } catch (DataIntegrityViolationException e) {
            result.reject("error.vaga", e.getMessage() != null ? e.getMessage() : "Erro ao salvar vaga.");
            return "form";
        }
        return "redirect:/vaga";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        vagaService.deleteById(id);
        var message = messageSource.getMessage("delete", null, LocaleContextHolder.getLocale());
        redirect.addFlashAttribute("message", message + " realizada com sucesso!");
        return "redirect:/vaga";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Vaga vaga = vagaService.findById(id);
        model.addAttribute("vaga", vaga);
        return "form";
    }

    @PostMapping("/edit/{id}")
    public String updateVaga(@PathVariable Long id,
                             @Valid @ModelAttribute Vaga vagaAtualizada,
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
        return "redirect:/vaga";
    }

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
        var avatar = user.getAttribute("picture") != null ? user.getAttribute("picture") : user.getAttribute("avatar_url");
        model.addAttribute("avatar", avatar);
        return "index";
    }
}
