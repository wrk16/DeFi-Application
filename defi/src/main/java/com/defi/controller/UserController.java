package com.defi.controller;

import com.defi.entity.User;
import com.defi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping({"","/"})
    public List<User> getAll(){
        return userService.getAll();
    }

    @PostMapping({"/register","register%0A"})
    public ResponseEntity<?> registerUser(@RequestBody User userd ) {
        try {
            User user = userService.registerUser(userd.getName(), userd.getEmail(), userd.getPassword());
            return ResponseEntity.ok("User registered successfully with wallet: " + user.getWalletAddress());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{email}")
    public String getUserWalletByEmail(@PathVariable String email) {
        Optional<User> user = userService.getUserByEmail(email.trim());
        return user.get().getWalletAddress();
    }


    @GetMapping("/balance/{email}")
    public BigDecimal getUserBalance(@PathVariable String email) throws Exception {
        String wallet=userService.getUserByEmail(email.trim()).get().getWalletAddress();
        return userService.getUserBalance(wallet);
    }
}




