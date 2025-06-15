package com.defi.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loans")
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "borrower_id", nullable = false)
    private User borrower;
    
    @ManyToOne
    @JoinColumn(name = "lender_id")
    private User lender;  // Initially null
    
    @Column(nullable = false)
    private BigDecimal loanAmount; // Number (e.g., points, credits)
    
    @Column(nullable = false)
    private BigDecimal collateralEth; // Ethereum amount (ETH)
    
    @Column(nullable = false)
    private LocalDateTime requestTime;
    
    @Column(nullable = false)
    private LocalDateTime deadline; // Repayment deadline
    
    @Column
    private LocalDateTime repaidDate; // Date when loan was repaid (null if not repaid)
    
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private LoanStatus status;  // PENDING, ACTIVE, REPAID, DEFAULTED
    
    public void setDaysUntilDeadline(long daysUntil) {
    }
    
    public enum LoanStatus {
        PENDING, ACTIVE, REPAID, DEFAULTED
    }
    
    public Loan() {}
    
    public Loan(User borrower, BigDecimal loanAmount, BigDecimal collateralEth, LocalDateTime deadline) {
        this.borrower = borrower;
        this.loanAmount = loanAmount;
        this.collateralEth = collateralEth;
        this.requestTime = LocalDateTime.now();
        this.deadline = deadline;
        this.status = LoanStatus.PENDING;
        this.repaidDate = null; // Initially null
    }
    
    // Getters & Setters...
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getBorrower() {
        return borrower;
    }
    
    public void setBorrower(User borrower) {
        this.borrower = borrower;
    }
    
    public User getLender() {
        return lender;
    }
    
    public void setLender(User lender) {
        this.lender = lender;
    }
    
    public BigDecimal getLoanAmount() {
        return loanAmount;
    }
    
    public void setLoanAmount(BigDecimal loanAmount) {
        this.loanAmount = loanAmount;
    }
    
    public BigDecimal getCollateralEth() {
        return collateralEth;
    }
    
    public void setCollateralEth(BigDecimal collateralEth) {
        this.collateralEth = collateralEth;
    }
    
    public LocalDateTime getRequestTime() {
        return requestTime;
    }
    
    public void setRequestTime(LocalDateTime requestTime) {
        this.requestTime = requestTime;
    }
    
    public LocalDateTime getDeadline() {
        return deadline;
    }
    
    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }
    
    public LocalDateTime getRepaidDate() {
        return repaidDate;
    }
    
    public void setRepaidDate(LocalDateTime repaidDate) {
        this.repaidDate = repaidDate;
    }
    
    public LoanStatus getStatus() {
        return status;
    }
    
    public void setStatus(LoanStatus status) {
        this.status = status;
    }
}