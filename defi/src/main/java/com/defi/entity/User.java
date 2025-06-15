package com.defi.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

// 1000    2eth   accept(lender 1000withdraw and borrower recieves/
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String password; // Store hashed passwords

    @Column(nullable = false, unique = true)
    private String walletAddress; // Ethereum wallet address

    @Column(nullable = false)
    private String privateKey;

    @Column(nullable = false)
    private BigDecimal stableBalance = BigDecimal.ZERO; // Added field for lending balance

    public User() {}

    public User(String name, String email, String password, String walletAddress, String privateKey, BigDecimal balance) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.walletAddress = walletAddress;
        this.privateKey = privateKey;
        this.stableBalance = balance;
    }

    // Getters & Setters
    public BigDecimal getBalance() {
        return stableBalance;
    }

    public void deposit(BigDecimal amount) {
        this.stableBalance = this.stableBalance.add(amount);
    }

    public boolean withdraw(BigDecimal amount) {
        if (this.stableBalance.compareTo(amount) >= 0) {
            this.stableBalance = this.stableBalance.subtract(amount);
            return true;
        }
        return false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public void setBalance(BigDecimal balance) {
        this.stableBalance = balance;
    }
}