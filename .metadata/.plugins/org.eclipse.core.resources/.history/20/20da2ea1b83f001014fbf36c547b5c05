package com.defi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.exceptions.JsonRpcError;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

@Service
public class BlockchainService {

    @Autowired
    private Web3j web3j;

    public long getLatestBlockNumber() {
        try {
            EthBlockNumber blockNumber = web3j.ethBlockNumber().send();
            return blockNumber.getBlockNumber().longValue();
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch block number", e);
        }
    }

    /**
     * Get Ethereum balance of a user.
     */
    public BigDecimal getEthereumBalance(String walletAddress) throws Exception {
        BigInteger balanceInWei = web3j.ethGetBalance(walletAddress, DefaultBlockParameterName.LATEST)
                .send()
                .getBalance();
        return Convert.fromWei(new BigDecimal(balanceInWei), Convert.Unit.ETHER);
    }
    /**
     * Send Ethereum from one wallet to another.
     */
    public String sendEthereum(String senderPrivateKey, String receiverWallet, BigDecimal amount) throws Exception {
        Credentials credentials = Credentials.create(senderPrivateKey);

        // Fetch sender's nonce (prevents duplicate transactions)
        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                credentials.getAddress(), DefaultBlockParameterName.LATEST).send();
        BigInteger nonce = ethGetTransactionCount.getTransactionCount();

        // Convert ETH amount to Wei (smallest unit of ETH)
        BigInteger value = Convert.toWei(amount, Convert.Unit.ETHER).toBigInteger();

        // Set gas parameters
        BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
        BigInteger gasLimit = DefaultGasProvider.GAS_LIMIT;

        // Create raw transaction
        RawTransaction rawTransaction = RawTransaction.createEtherTransaction(
                nonce, gasPrice, gasLimit, receiverWallet, value);

        // Sign the transaction
        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction,11155111, credentials);
        String hexValue = Numeric.toHexString(signedMessage);

        //  Send the transaction
        EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();

        if (ethSendTransaction.hasError()) {
            throw new RuntimeException("Transaction Error: " + ethSendTransaction.getError().getMessage());
        }

        return ethSendTransaction.getTransactionHash();
    }



}
