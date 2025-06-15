package com.defi.service;

import com.defi.entity.Loan;
import com.defi.entity.User;
import com.defi.repository.LoanRepository;
import com.defi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class LoanService {

    private static final Logger logger = LoggerFactory.getLogger(LoanService.class);

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    BlockchainService blockchainService;

    public Loan requestLoan(String borrowerEmail, BigDecimal loanAmount, BigDecimal collateralEth, int durationDays) throws Exception {
        User borrower = userRepository.findByEmail(borrowerEmail)
                .orElseThrow(() -> new NoSuchElementException("Borrower not found"));

        // Check if borrower has sufficient ETH collateral
        BigDecimal borrowerEthBalance = blockchainService.getEthereumBalance(borrower.getWalletAddress());
        if (borrowerEthBalance.compareTo(collateralEth) < 0) {
            throw new IllegalStateException("Insufficient ETH balance for collateral. Required: " + 
                collateralEth + ", Available: " + borrowerEthBalance);
        }

        Loan loan = new Loan(borrower, loanAmount, collateralEth, LocalDateTime.now().plusDays(durationDays));
        return loanRepository.save(loan);
    }

    public List<Loan> getAvailableLoans() {
        return loanRepository.findByStatus(Loan.LoanStatus.PENDING);
    }
    
    public List<Loan> getAvailableLoansExcludingCurrentUser(String email) {
        return loanRepository.findByStatusAndBorrower_EmailNot(Loan.LoanStatus.PENDING, email);
    }

    public Loan getLoanById(Long loanId) {
        return loanRepository.findById(loanId).orElse(null);
    }

    public Loan acceptLoan(String lenderEmail, Long loanId) throws Exception {
        User lender = userRepository.findByEmail(lenderEmail)
                .orElseThrow(() -> new NoSuchElementException("Lender not found"));

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new NoSuchElementException("Loan request not found"));

        if (loan.getStatus() != Loan.LoanStatus.PENDING) {
            throw new IllegalStateException("Loan is no longer available");
        }

        // CRITICAL FIX: Check if lender has sufficient stable coin balance
        if (lender.getBalance().compareTo(loan.getLoanAmount()) < 0) {
            throw new IllegalStateException("Insufficient stable coin balance. Required: " + 
                loan.getLoanAmount() + ", Available: " + lender.getBalance());
        }

        User borrower = loan.getBorrower();

        // FIXED: Only transfer stable coins, NO ETH transaction here
        // Transfer stable coins from lender to borrower
        lender.withdraw(loan.getLoanAmount());
        borrower.deposit(loan.getLoanAmount());

        // Update both users in database
        userRepository.save(lender);
        userRepository.save(borrower);

        // Update Loan Status - NO ETH TRANSFER AT THIS POINT
        loan.setLender(lender);
        loan.setStatus(Loan.LoanStatus.ACTIVE);
        return loanRepository.save(loan);
    }

    public List<Loan> getLoanHistoryForUser(String email, String string) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new NoSuchElementException("User not found"));
        if(string=="requested")
        return loanRepository.findByBorrowerAndStatusIn(
            user,
            List.of(Loan.LoanStatus.PENDING, Loan.LoanStatus.ACTIVE, Loan.LoanStatus.REPAID,Loan.LoanStatus.DEFAULTED)
        );
        else 
        	return loanRepository.findByLenderAndStatusIn(
                    user,
                    List.of(Loan.LoanStatus.PENDING, Loan.LoanStatus.ACTIVE, Loan.LoanStatus.REPAID,Loan.LoanStatus.DEFAULTED)
                );
    }

    public void deletePendingLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new NoSuchElementException("Loan not found"));

        if (loan.getStatus() != Loan.LoanStatus.PENDING) {
            throw new IllegalStateException("Only PENDING loans can be deleted.");
        }

        loanRepository.delete(loan);
    }

    public void repayLoan(Long loanId) throws Exception {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new NoSuchElementException("Loan not found"));

        if (loan.getStatus() != Loan.LoanStatus.ACTIVE) {
            throw new IllegalStateException("Loan is not active");
        }

        // Check if loan has exceeded deadline
        User borrower = loan.getBorrower();
        User lender = loan.getLender();

        if (LocalDateTime.now().isAfter(loan.getDeadline())) {
            liquidateLoan(loanId);
            return;
        }

        // Verify borrower still has sufficient ETH for collateral
        BigDecimal borrowerEthBalance = blockchainService.getEthereumBalance(borrower.getWalletAddress());
        if (borrowerEthBalance.compareTo(loan.getCollateralEth()) < 0) {
            throw new IllegalStateException("Borrower has insufficient ETH balance for collateral.");
        }

        // Check if borrower has sufficient balance to repay
        if (borrower.getBalance().compareTo(loan.getLoanAmount()) < 0) {
            throw new IllegalStateException("Insufficient stable coin balance to repay loan.");
        }

        // FIXED: Only transfer stable coins, NO ETH transaction here
        // Transfer stable coins from borrower back to lender
        borrower.withdraw(loan.getLoanAmount());
        lender.deposit(loan.getLoanAmount());

        // Update both users in database
        userRepository.save(borrower);
        userRepository.save(lender);

        // Mark loan as repaid and set repaid date
        loan.setStatus(Loan.LoanStatus.REPAID);
        loan.setRepaidDate(LocalDateTime.now());
        loanRepository.save(loan);
    }
    
    public void liquidateLoan(Long loanId) throws Exception {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new NoSuchElementException("Loan not found"));

        if (loan.getStatus() != Loan.LoanStatus.ACTIVE) {
            throw new IllegalStateException("Loan is not active");
        }

        // FIXED: Only allow liquidation if deadline has passed
        if (LocalDateTime.now().isBefore(loan.getDeadline())) {
            throw new IllegalStateException("Loan deadline has not passed yet. Cannot liquidate.");
        }

        User borrower = loan.getBorrower();
        User lender = loan.getLender();

        // CORRECT: ETH transaction only happens during liquidation (deadline exceeded)
        // Transfer ETH collateral from borrower to lender
        String txHash = blockchainService.sendEthereum(
                borrower.getPrivateKey(),  // Borrower's ETH goes to lender
                lender.getWalletAddress(),
                loan.getCollateralEth()
        );

        if (txHash == null) {
            throw new RuntimeException("ETH collateral transfer failed during liquidation.");
        }

        // Mark loan as DEFAULTED
        loan.setStatus(Loan.LoanStatus.DEFAULTED);
        loan.setRepaidDate(LocalDateTime.now());
        loanRepository.save(loan);
    }

    // ==================== AUTOMATION METHODS ====================

    /**
     * Automated task that runs every 5 minutes to check for:
     * 1. Active loans that have exceeded their deadline (auto-liquidate)
     * 2. Pending loans that have exceeded their deadline (auto-delete)
     */
    @Scheduled(fixedRate = 300000) // Run every 5 minutes (300,000 ms)
    @Transactional
    public void processExpiredLoans() {
        logger.info("Starting automated loan processing...");
        
        try {
            autoLiquidateExpiredActiveLoans();
            autoDeleteExpiredPendingLoans();
        } catch (Exception e) {
            logger.error("Error during automated loan processing: ", e);
        }
        
        logger.info("Completed automated loan processing.");
    }

    /**
     * Auto-liquidate active loans that have exceeded their deadline
     */
    @Transactional
    public void autoLiquidateExpiredActiveLoans() {
        List<Loan> expiredActiveLoans = loanRepository.findByStatusAndDeadlineBefore(
            Loan.LoanStatus.ACTIVE, 
            LocalDateTime.now()
        );

        logger.info("Found {} active loans that have exceeded deadline", expiredActiveLoans.size());

        for (Loan loan : expiredActiveLoans) {
            try {
                logger.info("Auto-liquidating loan ID: {} for borrower: {}", 
                    loan.getId(), loan.getBorrower().getEmail());
                
                liquidateLoan(loan.getId());
                
                logger.info("Successfully liquidated loan ID: {}", loan.getId());
            } catch (Exception e) {
                logger.error("Failed to auto-liquidate loan ID: {} - Error: {}", 
                    loan.getId(), e.getMessage());
            }
        }
    }

    /**
     * Auto-delete pending loans that have exceeded their deadline
     */
    @Transactional
    public void autoDeleteExpiredPendingLoans() {
        List<Loan> expiredPendingLoans = loanRepository.findByStatusAndDeadlineBefore(
            Loan.LoanStatus.PENDING, 
            LocalDateTime.now()
        );

        logger.info("Found {} pending loans that have exceeded deadline", expiredPendingLoans.size());

        for (Loan loan : expiredPendingLoans) {
            try {
                logger.info("Auto-deleting expired pending loan ID: {} for borrower: {}", 
                    loan.getId(), loan.getBorrower().getEmail());
                
                loanRepository.delete(loan);
                
                logger.info("Successfully deleted expired pending loan ID: {}", loan.getId());
            } catch (Exception e) {
                logger.error("Failed to auto-delete pending loan ID: {} - Error: {}", 
                    loan.getId(), e.getMessage());
            }
        }
    }

    /**
     * Manual trigger for processing expired loans (useful for testing or admin actions)
     */
    public void manualProcessExpiredLoans() {
        logger.info("Manual trigger for processing expired loans");
        processExpiredLoans();
    }

    /**
     * Get count of loans that will be processed in next automation cycle
     */
    public int getExpiredLoansCount() {
        LocalDateTime now = LocalDateTime.now();
        
        int expiredActiveCount = loanRepository.findByStatusAndDeadlineBefore(
            Loan.LoanStatus.ACTIVE, now).size();
        
        int expiredPendingCount = loanRepository.findByStatusAndDeadlineBefore(
            Loan.LoanStatus.PENDING, now).size();
        
        return expiredActiveCount + expiredPendingCount;
    }
}