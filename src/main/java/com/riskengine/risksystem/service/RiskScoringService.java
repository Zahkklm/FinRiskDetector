package com.riskengine.risksystem.service;

import com.riskengine.risksystem.model.RiskLevel;
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
     * Evaluates risk for a transaction and returns a RiskScore object
     * This method is used by TradingService
     * 
     * @param transaction The transaction to evaluate
     * @param userProfile The user profile associated with the transaction
     * @return A RiskScore object with the calculated score and risk level
     */
    public RiskScore calculateRiskScore(Transaction transaction, UserProfile userProfile) {
        // Calculate numeric score using existing method
        double score = calculateNumericScore(transaction, userProfile);
        RiskLevel riskLevel = determineRiskLevel(score);
        
        return RiskScore.builder()
                .transactionId(transaction.getId())
                .score(score)
                .level(riskLevel)
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * Creates a RiskScore object for a given transaction
     * 
     * @param transaction The transaction to evaluate
     * @return A RiskScore object with the calculated score and risk level
     */
    public RiskScore createRiskScore(Transaction transaction) {
        double score = calculateNumericScore(transaction);
        RiskLevel riskLevel = determineRiskLevel(score);
        
        return RiskScore.builder()
                .transactionId(transaction.getId())
                .score(score)
                .level(riskLevel)
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * Determines the risk level based on the numeric score
     */
    private RiskLevel determineRiskLevel(double score) {
        if (score < 0.3) {
            return RiskLevel.LOW;
        } else if (score < 0.7) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.HIGH;
        }
    }

    /**
     * Calculate numeric risk score from a RiskScore object
     * @return Double risk score value
     */
    public double calculateNumericScore(RiskScore riskScore) {
        Transaction transaction = transactionRepository.findById(riskScore.getTransactionId())
            .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));
        return calculateNumericScore(transaction);
    }

    /**
     * Calculate numeric risk score from a Transaction
     * @return Double risk score value
     */
    public double calculateNumericScore(Transaction transaction) {
        UserProfile userProfile = userProfileRepository.findById(transaction.getUserId())
            .orElse(null);
        return calculateNumericScore(transaction, userProfile);
    }
    
    /**
     * Original method that calculates risk based on both transaction and user profile
     * @return Double risk score value
     */
    public double calculateNumericScore(Transaction transaction, UserProfile userProfile) {
        double baseScore = mlModelUtil.calculateRiskScore(transaction);
        
        if (userProfile != null && userProfile.isHighRisk()) {
            baseScore += 0.2;
        }
        
        return Math.min(baseScore, 1.0);
    }
}