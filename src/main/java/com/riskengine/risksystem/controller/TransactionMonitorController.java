package com.riskengine.risksystem.controller;

import com.riskengine.risksystem.model.Transaction;
import com.riskengine.risksystem.service.TransactionProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionMonitorController {

    private final TransactionProcessingService transactionProcessingService;

    @Autowired
    public TransactionMonitorController(TransactionProcessingService transactionProcessingService) {
        this.transactionProcessingService = transactionProcessingService;
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        List<Transaction> transactions = transactionProcessingService.getAllTransactions();
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable Long id) {
        Transaction transaction = transactionProcessingService.getTransactionById(id);
        return transaction != null ? ResponseEntity.ok(transaction) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Transaction> createTransaction(@RequestBody Transaction transaction) {
        transactionProcessingService.processTransaction(transaction);
        return ResponseEntity.ok(transaction);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        transactionProcessingService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }
}