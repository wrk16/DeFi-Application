package com.defi.repository;


import com.defi.entity.Transaction;
import com.defi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findBySender(User sender);
    
    List<Transaction> findByReceiver(User receiver);
    
    Optional<Transaction> findByTransactionHash(String transactionHash);
}
