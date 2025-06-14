package com.riskengine.risksystem.service;

import com.riskengine.risksystem.model.Transaction;
import com.riskengine.risksystem.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionProcessingService {

    private final TransactionRepository transactionRepository;
    private final RiskScoringService riskScoringService;

    @Autowired
    public TransactionProcessingService(TransactionRepository transactionRepository, RiskScoringService riskScoringService) {
        this.transactionRepository = transactionRepository;
        this.riskScoringService = riskScoringService;
    }

    /**
     * Get all transactions
     */
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    /**
     * Get transaction by ID
     */
    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Transaction not found with id: " + id));
    }

    /**
     * Delete transaction by ID
     */
    public void deleteTransaction(Long id) {
        if (!transactionRepository.existsById(id)) {
            throw new EntityNotFoundException("Transaction not found with id: " + id);
        }
        transactionRepository.deleteById(id);
    }

    /**
     * Process a new transaction
     */
    @Transactional
    public void processTransaction(Transaction transaction) {
        // Validate the transaction
        validateTransaction(transaction);

        // First save to get an ID
        transaction = transactionRepository.save(transaction);
        
        // Calculate risk score
        double riskScore = riskScoringService.calculateRiskScore(transaction);

        // Update with risk score
        transaction.setRiskScore(riskScore);
        transactionRepository.save(transaction);
    }

    private void validateTransaction(Transaction transaction) {
        if (transaction.getAmount() == null || 
            transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transaction amount must be greater than zero.");
        }
        // Additional validation rules can be added here
    }
}