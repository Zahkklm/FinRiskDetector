package com.riskengine.risksystem.config;

import com.riskengine.risksystem.market.model.Asset;
import com.riskengine.risksystem.market.simulation.MarketSimulator;
import com.riskengine.risksystem.market.service.TradingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for market simulation and scheduling
 */
@Configuration
@EnableScheduling
public class MarketConfig {
    @Autowired
    private MarketSimulator marketSimulator;
    
    @Autowired
    private TradingService tradingService;
    
    /**
     * Initialize market with predefined assets on startup
     */
    @PostConstruct
    public void initializeMarket() {
        List<Asset> assets = new ArrayList<>();
        
        // Add cryptocurrencies
        assets.add(Asset.builder()
            .symbol("BTC-USD")
            .name("Bitcoin")
            .type(Asset.AssetType.CRYPTO)
            .currency("USD")
            .volatility(0.03) // 3% daily volatility
            .build());
            
        assets.add(Asset.builder()
            .symbol("ETH-USD")
            .name("Ethereum")
            .type(Asset.AssetType.CRYPTO)
            .currency("USD")
            .volatility(0.04) // 4% daily volatility
            .build());
            
        // Add stocks
        assets.add(Asset.builder()
            .symbol("AAPL")
            .name("Apple Inc.")
            .type(Asset.AssetType.STOCK)
            .currency("USD")
            .volatility(0.015) // 1.5% daily volatility
            .build());
            
        assets.add(Asset.builder()
            .symbol("MSFT")
            .name("Microsoft Corporation")
            .type(Asset.AssetType.STOCK)
            .currency("USD")
            .volatility(0.014) // 1.4% daily volatility
            .build());
            
        assets.add(Asset.builder()
            .symbol("AMZN")
            .name("Amazon.com Inc.")
            .type(Asset.AssetType.STOCK)
            .currency("USD")
            .volatility(0.018) // 1.8% daily volatility
            .build());
            
        assets.add(Asset.builder()
            .symbol("GOOGL")
            .name("Alphabet Inc.")
            .type(Asset.AssetType.STOCK)
            .currency("USD")
            .volatility(0.016) // 1.6% daily volatility
            .build());
            
        // Add commodities
        assets.add(Asset.builder()
            .symbol("GOLD")
            .name("Gold")
            .type(Asset.AssetType.COMMODITY)
            .currency("USD")
            .volatility(0.01) // 1% daily volatility
            .build());
            
        // Initialize the market
        marketSimulator.initializeMarket(assets);
    }
    
    /**
     * Update market prices every 5 seconds
     */
    @Scheduled(fixedRate = 5000)
    public void updateMarketPrices() {
        marketSimulator.updateMarketPrices();
    }
    
    /**
     * Process limit orders every 5 seconds
     */
    @Scheduled(fixedRate = 5000)
    public void processLimitOrders() {
        tradingService.processLimitOrders();
    }
}