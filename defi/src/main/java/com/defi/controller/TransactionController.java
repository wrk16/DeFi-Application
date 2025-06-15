package com.defi.controller;

import com.defi.entity.Transaction;
import com.defi.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @PostMapping("/send")
    public ResponseEntity<?> sendTransaction(
            @RequestParam String receiverWalletAddress,
            @RequestParam BigDecimal amount,
            Authentication authentication) {
        try {
            String senderEmail = authentication.getName();
            TransactionReceipt transactionReceipt = transactionService.sendTransaction(
                    senderEmail, receiverWalletAddress, amount
            );
            return ResponseEntity.ok("Transaction successful! Hash: " + transactionReceipt.getTransactionHash());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Transaction failed: " + e.getMessage());
        }
    }

    @GetMapping("/{email}")
    public ResponseEntity<List<Transaction>> getUserTransactions(@PathVariable String email) {
        List<Transaction> transactions = transactionService.getTransactionsByUser(email);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/me")
    public ResponseEntity<List<Transaction>> getMyTransactions(Authentication authentication) {
        String email = authentication.getName();
        List<Transaction> transactions = transactionService.getTransactionsByUser(email);
        return ResponseEntity.ok(transactions);
    }
}
