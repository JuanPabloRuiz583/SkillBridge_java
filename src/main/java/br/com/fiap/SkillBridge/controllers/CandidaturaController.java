package br.com.fiap.SkillBridge.controllers;

import br.com.fiap.SkillBridge.dto.CandidaturaDTO;
import br.com.fiap.SkillBridge.services.CandidaturaService;
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
@RequestMapping("/candidatura")
public class CandidaturaController {

    private final CandidaturaService candidaturaService;
    private final VagaService vagaService;
    private final MessageSource messageSource;

    public CandidaturaController(CandidaturaService candidaturaService, VagaService vagaService, MessageSource messageSource) {
        this.candidaturaService = candidaturaService;
        this.vagaService = vagaService;
        this.messageSource = messageSource;
    }

    @GetMapping
    public String index(Model model, @AuthenticationPrincipal OAuth2User user) {
        if (user == null) return "redirect:/login";
        List<CandidaturaDTO> candidaturas = candidaturaService.findAll();
        model.addAttribute("candidaturas", candidaturas);
        model.addAttribute("user", user);
        var avatar = user.getAttribute("picture") != null ? user.getAttribute("picture") : user.getAttribute("avatar_url");
        model.addAttribute("avatar", avatar);
        return "CandidaturaIndex";
    }

    @GetMapping("/form")
    public String form(Model model, CandidaturaDTO candidatura) {
        model.addAttribute("candidatura", candidatura == null ? new CandidaturaDTO() : candidatura);
        model.addAttribute("vagas", vagaService.getAllVagas());
        return "FormCandidatura";
    }

    @PostMapping("/form")
    public String saveCandidatura(@Valid @ModelAttribute("candidatura") CandidaturaDTO candidaturaDTO,
                                  BindingResult result,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("vagas", vagaService.getAllVagas());
            return "FormCandidatura";
        }
        try {
            candidaturaService.create(candidaturaDTO);
            redirectAttributes.addFlashAttribute("message", "Candidatura criada com sucesso");
        } catch (DataIntegrityViolationException e) {
            result.reject("error.candidatura", e.getMessage() != null ? e.getMessage() : "Erro ao salvar candidatura.");
            model.addAttribute("vagas", vagaService.getAllVagas());
            return "FormCandidatura";
        }
        return "redirect:/candidatura";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        CandidaturaDTO dto = candidaturaService.findById(id);
        model.addAttribute("candidatura", dto);
        model.addAttribute("vagas", vagaService.getAllVagas());
        return "FormCandidatura";
    }

    @PostMapping("/edit/{id}")
    public String updateCandidatura(@PathVariable Long id,
                                    @Valid @ModelAttribute("candidatura") CandidaturaDTO candidaturaDTO,
                                    BindingResult result,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("vagas", vagaService.getAllVagas());
            return "FormCandidatura";
        }
        candidaturaService.update(id, candidaturaDTO);
        return "redirect:/candidatura";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        candidaturaService.delete(id);
        String message = messageSource.getMessage("delete", null, "Candidatura excluída", LocaleContextHolder.getLocale());
        redirectAttributes.addFlashAttribute("message", message);
        return "redirect:/candidatura";
    }
}
