package mg.itu.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller 
public class LoginController {

    @GetMapping("/")
    public String index() {
        return "views/auth/login";
    }
}