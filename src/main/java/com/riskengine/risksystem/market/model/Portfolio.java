package com.riskengine.risksystem.market.model;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a user's portfolio of assets
 */
@Data
public class Portfolio {
    /** User ID */
    private final String userId;
    
    /** Current cash balance */
    private double cashBalance;
    
    /** Holdings (symbol → quantity) */
    private final Map<String, Double> holdings = new HashMap<>();
    
    /**
     * Calculate total portfolio value based on current prices
     * 
     * @param prices Current market prices
     * @return Total portfolio value including cash
     */
    public double calculateTotalValue(Map<String, AssetPrice> prices) {
        double assetValue = holdings.entrySet().stream()
            .mapToDouble(entry -> {
                String symbol = entry.getKey();
                Double quantity = entry.getValue();
                AssetPrice price = prices.get(symbol);
                
                return price != null ? price.getPrice() * quantity : 0.0;
            })
            .sum();
        
        return cashBalance + assetValue;
    }
    
    /**
     * Add an asset to the portfolio
     * 
     * @param symbol Asset symbol
     * @param quantity Amount to add
     */
    public void addHolding(String symbol, double quantity) {
        holdings.put(symbol, holdings.getOrDefault(symbol, 0.0) + quantity);
    }
    
    /**
     * Remove an asset from the portfolio
     * 
     * @param symbol Asset symbol
     * @param quantity Amount to remove
     * @return true if successful, false if insufficient balance
     */
    public boolean removeHolding(String symbol, double quantity) {
        double currentQuantity = holdings.getOrDefault(symbol, 0.0);
        
        if (currentQuantity < quantity) {
            return false; // Insufficient balance
        }
        
        double newQuantity = currentQuantity - quantity;
        
        if (newQuantity <= 0) {
            holdings.remove(symbol);
        } else {
            holdings.put(symbol, newQuantity);
        }
        
        return true;
    }
    
    /**
     * Add cash to the portfolio
     * 
     * @param amount Amount to add
     */
    public void depositCash(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        cashBalance += amount;
    }
    
    /**
     * Remove cash from the portfolio
     * 
     * @param amount Amount to remove
     * @return true if successful, false if insufficient balance
     */
    public boolean withdrawCash(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        
        if (cashBalance < amount) {
            return false; // Insufficient balance
        }
        
        cashBalance -= amount;
        return true;
    }
}