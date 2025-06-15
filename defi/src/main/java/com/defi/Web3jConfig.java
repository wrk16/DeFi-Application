package com.defi;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;

import java.math.BigDecimal;

@Configuration
public class Web3jConfig {

    private static final String INFURA_URL = "https://sepolia.infura.io/v3/55d33ba38a934e72901c512d26818416"; // Use your own Infura/Alchemy node

    @Bean
    public Web3j get(){
        return web3j;
    }
    public Web3j web3j = Web3j.build(new HttpService(INFURA_URL));

    public BigDecimal getUserBalance(String walletAddress) throws Exception {
        BigDecimal balanceInWei = new BigDecimal(
                web3j.ethGetBalance(walletAddress, DefaultBlockParameterName.LATEST)
                        .send()
                        .getBalance()
        );
        return Convert.fromWei(balanceInWei, Convert.Unit.ETHER); // Convert to ETH
    }
}
