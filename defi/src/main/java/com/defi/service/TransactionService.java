package com.defi.service;

import com.defi.entity.Transaction;
import com.defi.entity.User;
import com.defi.repository.TransactionRepository;
import com.defi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class TransactionService {

    @Autowired
    private Web3j web3j;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private static final String WALLET_DIR = "wallets/";

    public TransactionReceipt sendTransaction(String senderEmail, String receiverWalletAddress, BigDecimal amount) throws Exception {
        // 1️⃣ Fetch Sender & Receiver
        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
        System.out.println("Sender found");

        User receiver = userRepository.findByWalletAddress(receiverWalletAddress)
                .orElseThrow(() -> new NoSuchElementException("Receiver not found"));
        System.out.println("Receiver found");

        // 2️⃣ Load Sender's Private Key
        Credentials credentials = Credentials.create(sender.getPrivateKey());
        System.out.println("Sender key retrieved");

        // 3️⃣ Fetch Current Gas Price and Increase It

        // 4️⃣ Fetch Nonce (Prevents Stuck Transactions)
        BigInteger nonce = web3j.ethGetTransactionCount(sender.getWalletAddress(), DefaultBlockParameterName.PENDING)
                .send()
                .getTransactionCount();
        System.out.println("Nonce: " + nonce);

        RawTransactionManager txManager = new RawTransactionManager(web3j, credentials, 11155111);

        BigInteger value = Convert.toWei(amount, Convert.Unit.ETHER).toBigInteger();
        BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice().multiply(BigInteger.valueOf(2));
        BigInteger gasLimit = BigInteger.valueOf(21000);

        RawTransaction rawTransaction = RawTransaction.createEtherTransaction(
                nonce, gasPrice, gasLimit, receiverWalletAddress, value
        );

        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, 11155111, credentials);
        String hexValue = Numeric.toHexString(signedMessage);

        EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();
        if (ethSendTransaction.hasError()) {
            System.err.println("Transaction Error: " + ethSendTransaction.getError().getMessage());
        } else {
            System.out.println("Transaction Hash: " + ethSendTransaction.getTransactionHash());
        }
        // 6️⃣ Send Funds
        Transfer transfer = new Transfer(web3j, txManager);
        TransactionReceipt receipt = transfer.sendFunds(
                receiverWalletAddress, amount, Convert.Unit.ETHER,gasPrice, gasLimit
        ).send();
        System.out.println("Transaction submitted");

        // 7️⃣ Wait for Confirmation (Optional)
        String txHash = receipt.getTransactionHash();
        System.out.println("Transaction Hash: " + txHash);

        for (int i = 0; i < 10; i++) { // Retry 10 times, waiting for confirmation
            Thread.sleep(5000); // Wait 5 seconds
            var txStatus = web3j.ethGetTransactionByHash(txHash).send().getTransaction();
            if (txStatus.isPresent() && txStatus.get().getBlockNumber() != null) {
                System.out.println("Transaction confirmed in block: " + txStatus.get().getBlockNumber());
                break;
            }
            System.out.println("Waiting for confirmation...");
        }

        // 8️⃣ Save Transaction to Database
        Transaction transaction = new Transaction(sender, receiver, amount, txHash, LocalDateTime.now());
        transactionRepository.save(transaction);

        return receipt;
    }



    public List<Transaction> getTransactionsByUser(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        User user = userOpt.get();
        return transactionRepository.findBySender(user);
    }
}
