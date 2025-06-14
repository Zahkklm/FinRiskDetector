package com.riskengine.risksystem.controller;

import com.riskengine.risksystem.model.RiskScore;
import com.riskengine.risksystem.model.Transaction;
import com.riskengine.risksystem.repository.TransactionRepository;
import com.riskengine.risksystem.service.RiskScoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/api/risk-assessment")
public class RiskAssessmentController {

    private final RiskScoringService riskScoringService;
    private final TransactionRepository transactionRepository;

    @Autowired
    public RiskAssessmentController(RiskScoringService riskScoringService, 
                                   TransactionRepository transactionRepository) {
        this.riskScoringService = riskScoringService;
        this.transactionRepository = transactionRepository;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<RiskScore> evaluateRisk(@RequestBody RiskScore riskScoreRequest) {
        // Find the transaction associated with this risk score
        Transaction transaction = transactionRepository.findById(riskScoreRequest.getTransactionId())
            .orElseThrow(() -> new EntityNotFoundException("Transaction not found with id: " + 
                                                         riskScoreRequest.getTransactionId()));
        
        // Create a proper risk score
        RiskScore evaluatedRiskScore = riskScoringService.createRiskScore(transaction);
        return ResponseEntity.ok(evaluatedRiskScore);
    }

    // Alternative approach that directly accepts a transaction
    @PostMapping("/evaluate-transaction")
    public ResponseEntity<RiskScore> evaluateTransactionRisk(@RequestBody Transaction transaction) {
        RiskScore evaluatedRiskScore = riskScoringService.createRiskScore(transaction);
        return ResponseEntity.ok(evaluatedRiskScore);
    }

    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok("Risk Assessment Service is running");
    }
}