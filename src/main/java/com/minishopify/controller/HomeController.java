package com.minishopify.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Landing page. Placeholder until the first real feature lands, so that the deployed
 * app on Azure always has something to show.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

}
