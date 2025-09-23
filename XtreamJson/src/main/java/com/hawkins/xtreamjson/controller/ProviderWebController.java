package com.hawkins.xtreamjson.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hawkins.xtreamjson.model.IptvProvider;
import com.hawkins.xtreamjson.service.IptvProviderService;

@Controller
@RequestMapping("/providers")
public class ProviderWebController {
    @Autowired
    private IptvProviderService service;

    @GetMapping
    public String listProviders(Model model) {
        model.addAttribute("providers", service.getAllProviders());
        return "providers";
    }

    @GetMapping("/edit/{id}")
    public String editProvider(@PathVariable Long id, Model model) {
        model.addAttribute("providers", service.getAllProviders());
        model.addAttribute("editProvider", service.getProvider(id).orElse(null));
        return "providers";
    }

    @PostMapping("/add")
    public String addProvider(@RequestParam String apiUrl, @RequestParam String username, @RequestParam String password) {
        IptvProvider provider = new IptvProvider();
        provider.setApiUrl(apiUrl);
        provider.setUsername(username);
        provider.setPassword(password);
        IptvProvider saved = service.saveProvider(provider);
        // Auto-select if no provider is selected
        if (service.getSelectedProvider().isEmpty()) {
            service.selectProvider(saved.getId());
        }
        return "redirect:/";
    }

    @PostMapping("/edit/{id}")
    public String updateProvider(@PathVariable Long id, @RequestParam String apiUrl, @RequestParam String username, @RequestParam String password) {
        service.getProvider(id).ifPresent(provider -> {
            provider.setApiUrl(apiUrl);
            provider.setUsername(username);
            provider.setPassword(password);
            service.saveProvider(provider);
        });
        return "redirect:/providers";
    }

    @GetMapping("/delete/{id}")
    public String deleteProvider(@PathVariable Long id) {
        boolean wasSelected = service.getProvider(id).map(IptvProvider::isSelected).orElse(false);
        service.deleteProvider(id);
        // If deleted provider was selected, select another if available
        if (wasSelected) {
            service.getAllProviders().stream().findFirst().ifPresent(p -> service.selectProvider(p.getId()));
        }
        return "redirect:/providers";
    }

    @GetMapping("/select/{id}")
    public String selectProvider(@PathVariable Long id) {
        service.selectProvider(id);
        return "redirect:/providers";
    }

    @PostMapping("/select/{id}")
    @ResponseBody
    public void selectProviderAjax(@PathVariable Long id) {
        service.selectProvider(id);
    }

    @GetMapping("/debug/providers")
    @ResponseBody
    public String debugProviders() {
        StringBuilder sb = new StringBuilder();
        service.getAllProviders().forEach(p -> {
            sb.append("ID: ").append(p.getId())
              .append(", API: ").append(p.getApiUrl())
              .append(", Username: ").append(p.getUsername())
              .append(", Selected: ").append(p.isSelected())
              .append("<br>");
        });
        return sb.toString();
    }

    @GetMapping("/fragment")
    public String providersFragment(Model model) {
        model.addAttribute("providers", service.getAllProviders());
        return "providers :: providersFragment";
    }
}