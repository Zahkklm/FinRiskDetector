package com.riskengine.risksystem.market.simulation;

import com.riskengine.risksystem.market.model.Asset;
import com.riskengine.risksystem.market.model.AssetPrice;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simulates a financial market with price movements and order matching
 */
@Service
@Slf4j
public class MarketSimulator {
    /** Current prices for all assets */
    private final Map<String, AssetPrice> currentPrices = new ConcurrentHashMap<>();
    
    /** Historical price data */
    private final Map<String, List<AssetPrice>> priceHistory = new ConcurrentHashMap<>();
    
    /** Available assets in the market */
    private final Map<String, Asset> availableAssets = new ConcurrentHashMap<>();
    
    /** Random number generator for price movements */
    private final Random random = new Random();
    
    /** Maximum history length to maintain */
    private static final int MAX_HISTORY_SIZE = 1000;
    
    /**
     * Initialize the market with a set of assets
     * 
     * @param assets List of assets to add to the market
     */
    public void initializeMarket(List<Asset> assets) {
        log.info("Initializing market with {} assets", assets.size());
        
        for (Asset asset : assets) {
            availableAssets.put(asset.getSymbol(), asset);
            
            // Set initial price
            double initialPrice = generateInitialPrice(asset);
            AssetPrice price = new AssetPrice(
                asset.getSymbol(),
                initialPrice,
                initialPrice * 0.99, // Net price with 1% fee
                initialPrice * 0.98, // Low
                initialPrice * 1.02, // High
                random.nextDouble() * 1000000, // Volume
                LocalDateTime.now()
            );
            
            currentPrices.put(asset.getSymbol(), price);
            priceHistory.put(asset.getSymbol(), new ArrayList<>(List.of(price)));
            
            log.info("Asset {} initialized at price {}", asset.getSymbol(), initialPrice);
        }
    }
    
    /**
     * Generate a realistic initial price based on asset type
     */
    private double generateInitialPrice(Asset asset) {
        return switch (asset.getType()) {
            case STOCK -> 10 + random.nextDouble() * 990; // $10-$1000
            case CRYPTO -> asset.getSymbol().contains("BTC") ? 
                        20000 + random.nextDouble() * 20000 : // BTC: $20k-$40k
                        100 + random.nextDouble() * 4900;     // Other: $100-$5000
            case FOREX -> 0.5 + random.nextDouble() * 1.5;    // 0.5-2.0 rate
            case COMMODITY -> 50 + random.nextDouble() * 950; // $50-$1000
        };
    }
    
    /**
     * Get current price for a specific asset
     * 
     * @param symbol Asset symbol
     * @return Current price data
     * @throws IllegalArgumentException if the asset doesn't exist
     */
    public AssetPrice getCurrentPrice(String symbol) {
        if (!currentPrices.containsKey(symbol)) {
            throw new IllegalArgumentException("Asset not found: " + symbol);
        }
        return currentPrices.get(symbol);
    }
    
    /**
     * Get all current market prices
     * 
     * @return Map of all current prices by symbol
     */
    public Map<String, AssetPrice> getAllPrices() {
        return new HashMap<>(currentPrices);
    }
    
    /**
     * Get price history for a specific asset
     * 
     * @param symbol Asset symbol
     * @return List of historical prices
     * @throws IllegalArgumentException if the asset doesn't exist
     */
    public List<AssetPrice> getPriceHistory(String symbol) {
        if (!priceHistory.containsKey(symbol)) {
            throw new IllegalArgumentException("Asset not found: " + symbol);
        }
        return new ArrayList<>(priceHistory.get(symbol));
    }
    
    /**
     * Update all market prices - simulates market movements
     * This method would typically be called on a schedule
     */
    public void updateMarketPrices() {
        LocalDateTime now = LocalDateTime.now();
        
        for (String symbol : availableAssets.keySet()) {
            Asset asset = availableAssets.get(symbol);
            AssetPrice currentPrice = currentPrices.get(symbol);
            
            // Generate price movement based on asset volatility
            double priceMovement = generatePriceMovement(asset, currentPrice.getPrice());
            double newPrice = Math.max(0.01, currentPrice.getPrice() + priceMovement);
            
            // Update high/low if needed
            double newHigh = Math.max(currentPrice.getHigh(), newPrice);
            double newLow = Math.min(currentPrice.getLow(), newPrice);
            
            // Generate new volume
            double newVolume = currentPrice.getVolume() * (0.8 + random.nextDouble() * 0.4);
            
            // Create new price object
            AssetPrice newPriceObj = new AssetPrice(
                symbol,
                newPrice,
                newPrice * 0.99, // 1% fee
                newLow,
                newHigh,
                newVolume,
                now
            );
            
            // Update current price
            currentPrices.put(symbol, newPriceObj);
            
            // Update history
            List<AssetPrice> history = priceHistory.get(symbol);
            history.add(newPriceObj);
            
            // Trim history if needed
            if (history.size() > MAX_HISTORY_SIZE) {
                history.remove(0);
            }
            
            log.debug("Updated price for {}: {} -> {}", symbol, 
                     currentPrice.getPrice(), newPrice);
        }
        
        log.info("Market prices updated at {}", now);
    }
    
    /**
     * Generate a realistic price movement based on asset volatility
     */
    private double generatePriceMovement(Asset asset, double currentPrice) {
        // Base volatility from asset definition
        double baseVolatility = asset.getVolatility();
        
        // Random market factor (some days are more volatile)
        double marketFactor = 0.8 + (random.nextDouble() * 0.4);
        
        // Combine factors
        double effectiveVolatility = baseVolatility * marketFactor;
        
        // Generate random movement using normal distribution
        double randomFactor = random.nextGaussian();
        
        // Scale movement by price and volatility
        return currentPrice * effectiveVolatility * randomFactor;
    }
    
    /**
     * Get all available assets in the market
     */
    public List<Asset> getAvailableAssets() {
        return new ArrayList<>(availableAssets.values());
    }
}