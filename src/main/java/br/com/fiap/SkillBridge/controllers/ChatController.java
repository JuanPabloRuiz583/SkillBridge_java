package br.com.fiap.SkillBridge.controllers;

import br.com.fiap.SkillBridge.services.AIService;
import br.com.fiap.SkillBridge.dto.ChatRequest;
import br.com.fiap.SkillBridge.dto.ChatResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class ChatController {

    private final AIService aiService;

    public ChatController(AIService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/chat")
    public String chatPage(Model model, @AuthenticationPrincipal OAuth2User user) {
        if (user != null) {
            model.addAttribute("avatar", user.getAttribute("avatar_url"));
            model.addAttribute("username", user.getAttribute("name") != null ? user.getAttribute("name") : user.getAttribute("login"));
        }
        return "chat";
    }

    @PostMapping("/chat/api")
    @ResponseBody
    public ChatResponse chatApi(@RequestBody ChatRequest request) {
        String reply = aiService.ask(request.getMessage());
        return new ChatResponse(reply);
    }
}