package com.defi.controller;

import com.defi.entity.User;
import com.defi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.Optional;

@Controller
public class BalanceController {

    @Autowired
    private UserService userService;
    
    @GetMapping("/check-balance")
    public String showBalanceForm() {
        return "balance-check";
    }
    
    @PostMapping("/check-balance")
    public String checkBalance(Authentication authentication, Model model) {
        try {
            String email = authentication.getName();
            Optional<User> userOptional = userService.getUserByEmail(email);
            
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                // Get ETH balance from the blockchain
                BigDecimal ethBalance = userService.getUserBalance(user.getWalletAddress());
                // Get stable money balance from the database
                BigDecimal stableMoney = user.getBalance();
                
                model.addAttribute("balance", ethBalance);
                model.addAttribute("stableMoney", stableMoney);
            } else {
                model.addAttribute("error", "User not found. Please check your email address.");
            }
        } catch (Exception e) {
            model.addAttribute("error", "An error occurred: " + e.getMessage());
        }
        
        return "balance-check";
    }
}