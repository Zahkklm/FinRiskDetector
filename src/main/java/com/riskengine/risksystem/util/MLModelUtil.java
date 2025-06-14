package com.riskengine.risksystem.util;

import com.riskengine.risksystem.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MLModelUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(MLModelUtil.class);
    
    // Threshold for amount anomaly detection (transactions above this percentile are flagged)
    private static final double AMOUNT_PERCENTILE_THRESHOLD = 95.0;
    
    // Threshold for frequency anomaly detection (transactions per hour)
    private static final int FREQUENCY_THRESHOLD = 5;
    
    // Threshold for unusual hour (transactions outside normal business hours)
    private static final int HOUR_START = 7; // 7 AM
    private static final int HOUR_END = 23;  // 11 PM
    
    /**
     * Detects anomalies in a list of transactions using multiple detection methods
     * 
     * @param transactions List of transactions to analyze
     * @return List of transactions identified as anomalies
     */
    public List<Transaction> detectAnomalies(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return new ArrayList<>();
        }
        
        logger.info("Analyzing {} transactions for anomalies", transactions.size());
        
        List<Transaction> anomalies = new ArrayList<>();
        
        // Combine results from different detection methods
        anomalies.addAll(detectAmountAnomalies(transactions));
        anomalies.addAll(detectFrequencyAnomalies(transactions));
        anomalies.addAll(detectTimeAnomalies(transactions));
        
        // Remove duplicates (a transaction might be flagged by multiple methods)
        List<Transaction> uniqueAnomalies = anomalies.stream()
                .distinct()
                .collect(Collectors.toList());
        
        logger.info("Detected {} unique anomalous transactions", uniqueAnomalies.size());
        return uniqueAnomalies;
    }
    
    /**
     * Detects transactions with unusually high amounts
     */
    private List<Transaction> detectAmountAnomalies(List<Transaction> transactions) {
        // Sort transactions by amount
        List<Transaction> sortedByAmount = transactions.stream()
                .sorted((t1, t2) -> t1.getAmount().compareTo(t2.getAmount()))
                .collect(Collectors.toList());
        
        // Calculate threshold index for percentile
        int thresholdIndex = (int) Math.ceil(sortedByAmount.size() * AMOUNT_PERCENTILE_THRESHOLD / 100.0) - 1;
        if (thresholdIndex < 0) thresholdIndex = 0;
        
        // Get transactions above threshold
        return sortedByAmount.stream()
                .skip(thresholdIndex)
                .collect(Collectors.toList());
    }
    
    /**
     * Detects unusual transaction frequency by user
     */
    private List<Transaction> detectFrequencyAnomalies(List<Transaction> transactions) {
        // Group transactions by user and hour
        Map<String, List<Transaction>> userHourTransactions = new HashMap<>();
        
        for (Transaction transaction : transactions) {
            String key = transaction.getUserId() + "_" + 
                         transaction.getTimestamp().getYear() + "_" +
                         transaction.getTimestamp().getDayOfYear() + "_" +
                         transaction.getTimestamp().getHour();
            
            userHourTransactions.computeIfAbsent(key, k -> new ArrayList<>()).add(transaction);
        }
        
        // Find users with high transaction frequency in an hour
        List<Transaction> frequencyAnomalies = new ArrayList<>();
        userHourTransactions.forEach((key, userTransactions) -> {
            if (userTransactions.size() > FREQUENCY_THRESHOLD) {
                frequencyAnomalies.addAll(userTransactions);
            }
        });
        
        return frequencyAnomalies;
    }
    
    /**
     * Detects transactions at unusual times
     */
    private List<Transaction> detectTimeAnomalies(List<Transaction> transactions) {
        return transactions.stream()
                .filter(t -> {
                    int hour = t.getTimestamp().getHour();
                    return hour < HOUR_START || hour > HOUR_END;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Calculates risk score for a single transaction
     * 
     * @param transaction Transaction to analyze
     * @return Risk score between 0.0 (low risk) and 1.0 (high risk)
     */
    // in the calculateRiskScore method:

    public double calculateRiskScore(Transaction transaction) {
        if (transaction == null) {
            return 0.0;
        }
        
        double score = 0.0;
        
        // Fix: Use compareTo instead of operator >
        // Factor 1: Large amount (>$10,000)
        if (transaction.getAmount().compareTo(new BigDecimal("10000")) > 0) {
            score += 0.3;
        }
        
        // Factor 2: Transaction at unusual hours
        int hour = transaction.getTimestamp().getHour();
        if (hour < HOUR_START || hour > HOUR_END) {
            score += 0.2;
        }
        
        // Cap the score at 1.0
        return Math.min(score, 1.0);
    }
}