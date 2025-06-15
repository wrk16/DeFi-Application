package com.defi.controller;

import com.defi.service.BlockchainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/blockchain")
public class BlockchainController {

    @Autowired
    private BlockchainService blockchainService;

    @GetMapping("/latest-block")
    public String getLatestBlock() {
        return "Latest Ethereum Block Number: " + blockchainService.getLatestBlockNumber();
    }
}
