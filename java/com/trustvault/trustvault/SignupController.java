
package com.trustvault.trustvault;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SignupController {

    private final AppUserRepository repository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    public SignupController(
            AppUserRepository repository,
            CompanyRepository companyRepository,
            PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/signup")
    public String showSignupPage() {
        return "signup";
    }

    @PostMapping("/register")
    public String registerUser(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam String companyName,
            Model model,
            RedirectAttributes redirectAttributes) {

        username = username.trim();
        email = email.trim().toLowerCase();
        companyName = companyName.trim();

        if (username.isBlank() || email.isBlank()
                || password.isBlank() || companyName.isBlank()) {
            model.addAttribute("error",
                    "All fields are required.");
            return "signup";
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            model.addAttribute("error", "Please enter a valid email address.");
            return "signup";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            return "signup";
        }

        if (password.length() < 8) {
            model.addAttribute("error",
                    "Password must contain at least 8 characters.");
            return "signup";
        }

        if (repository.existsByUsername(username)) {
            model.addAttribute("error",
                    "Username already exists. Please choose another.");
            return "signup";
        }

        if (repository.findByEmail(email).isPresent()) {
            model.addAttribute("error",
                    "Email already registered. Please use another.");
            return "signup";
        }

        Company company = companyRepository.findByName(companyName)
                .orElse(null);

        if (company == null) {
            company = companyRepository.save(new Company(companyName));
        }

        AppUser user = new AppUser(
                username,
                passwordEncoder.encode(password));

        user.setEmail(email);
        user.setCompany(company);

        repository.save(user);

        redirectAttributes.addFlashAttribute(
                "registered",
                "Account created! Please log in.");

        return "redirect:/login?registered";
    }
}
