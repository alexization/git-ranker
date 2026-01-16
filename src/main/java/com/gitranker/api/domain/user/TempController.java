package com.gitranker.api.domain.user;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TempController {

    @GetMapping("/dashboard")
    public String dashboard() {
        return "index";
    }
}
