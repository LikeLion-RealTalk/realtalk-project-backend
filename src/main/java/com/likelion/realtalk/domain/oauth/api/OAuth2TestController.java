package com.likelion.realtalk.domain.oauth.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/oauth2")
public class OAuth2TestController {

    @GetMapping("/test")
    public String showTestPage() {
        return "oauth2-login-test";
    }

    @GetMapping("/setup-username")
    public String showUsernameSetupPage() {
        return "oauth2-username-setup";
    }

    @GetMapping("/success")
    public String loginSuccess(@RequestParam(required = false) String error) {
        if (error != null) {
            return "redirect:/oauth2/test?error=" + error;
        } else {
            return "redirect:/oauth2/test?success=true";
        }
    }
}
