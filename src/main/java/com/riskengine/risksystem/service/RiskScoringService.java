package com.riskengine.risksystem.service;

import com.riskengine.risksystem.model.RiskScore;
import com.riskengine.risksystem.model.Transaction;
import com.riskengine.risksystem.model.UserProfile;
import com.riskengine.risksystem.repository.TransactionRepository;
import com.riskengine.risksystem.repository.UserProfileRepository;
import com.riskengine.risksystem.util.MLModelUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;

@Service
public class RiskScoringService {

    private final UserProfileRepository userProfileRepository;
    private final TransactionRepository transactionRepository;
    private final MLModelUtil mlModelUtil;

    @Autowired
    public RiskScoringService(UserProfileRepository userProfileRepository, 
                             TransactionRepository transactionRepository,
                             MLModelUtil mlModelUtil) {
        this.userProfileRepository = userProfileRepository;
        this.transactionRepository = transactionRepository;
        this.mlModelUtil = mlModelUtil;
    }

    /**
     * Creates a RiskScore object for a given transaction
     * 
     * @param transaction The transaction to evaluate
     * @return A RiskScore object with the calculated score and risk level
     */
    public RiskScore createRiskScore(Transaction transaction) {
        double score = calculateRiskScore(transaction);
        String riskLevel = determineRiskLevel(score);
        
        return RiskScore.builder()
                .transactionId(transaction.getId())
                .score(score)
                .riskLevel(riskLevel)
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * Determines the risk level based on the numeric score
     */
    private String determineRiskLevel(double score) {
        if (score < 0.3) {
            return "LOW";
        } else if (score < 0.7) {
            return "MEDIUM";
        } else {
            return "HIGH";
        }
    }

    // The existing calculateRiskScore methods that return double
    
    /**
     * Calculate risk score from a RiskScore object
     */
    public double calculateRiskScore(RiskScore riskScore) {
        Transaction transaction = transactionRepository.findById(riskScore.getTransactionId())
            .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));
        return calculateRiskScore(transaction);
    }

    /**
     * Calculate risk score from a Transaction
     */
    public double calculateRiskScore(Transaction transaction) {
        UserProfile userProfile = userProfileRepository.findById(transaction.getUserId())
            .orElse(null);
        return calculateRiskScore(transaction, userProfile);
    }
    
    /**
     * Original method that calculates risk based on both transaction and user profile
     */
    public double calculateRiskScore(Transaction transaction, UserProfile userProfile) {
        double baseScore = mlModelUtil.calculateRiskScore(transaction);
        
        if (userProfile != null && userProfile.isHighRisk()) {
            baseScore += 0.2;
        }
        
        return Math.min(baseScore, 1.0);
    }
}