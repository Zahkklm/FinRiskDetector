package com.riskengine.risksystem.controller;

import com.riskengine.risksystem.dto.FundsRequestDTO;
import com.riskengine.risksystem.dto.OrderRequestDTO;
import com.riskengine.risksystem.market.model.*;
import com.riskengine.risksystem.market.service.*;
import com.riskengine.risksystem.market.simulation.MarketSimulator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

/**
 * REST API controller for market data and trading operations.
 * Provides endpoints for asset information, price data, trading, and portfolio management.
 */
@RestController
@RequestMapping("/api/market")
@Tag(name = "Market Operations", description = "APIs for market data, trading and portfolio management")
public class MarketController {
    @Autowired
    private MarketSimulator marketSimulator;
    
    @Autowired
    private TradingService tradingService;
    
    @Autowired
    private PortfolioService portfolioService;
    
    /**
     * Get all available assets in the market
     * 
     * @return List of all tradable assets with their details
     */
    @Operation(
        summary = "Get all available assets",
        description = "Returns a list of all assets that can be traded in the market"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved asset list",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Asset.class)
            )
        )
    })
    @GetMapping("/assets")
    public ResponseEntity<List<Asset>> getAssets() {
        return ResponseEntity.ok(marketSimulator.getAvailableAssets());
    }
    
    /**
     * Get current price for a specific asset
     * 
     * @param symbol The unique identifier for the asset (e.g., "BTC-USD")
     * @return Current price information for the requested asset
     * @throws 404 Not Found if the symbol doesn't exist
     */
    @Operation(
        summary = "Get price for specific asset",
        description = "Returns current price information for a specified asset"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved price information",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = AssetPrice.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Asset not found",
            content = @Content(schema = @Schema(hidden = true))
        )
    })
    @GetMapping("/price/{symbol}")
    public ResponseEntity<AssetPrice> getPrice(
            @Parameter(description = "Asset symbol, e.g. BTC-USD") 
            @PathVariable String symbol) {
        try {
            return ResponseEntity.ok(marketSimulator.getCurrentPrice(symbol));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get all current market prices
     * 
     * @return Map of all asset symbols to their current price information
     */
    @Operation(
        summary = "Get all market prices",
        description = "Returns current prices for all available assets in the market"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved all prices",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Object.class)
            )
        )
    })
    @GetMapping("/prices")
    public ResponseEntity<Map<String, AssetPrice>> getAllPrices() {
        return ResponseEntity.ok(marketSimulator.getAllPrices());
    }
    
    /**
     * Get price history for a specific asset
     * 
     * @param symbol The unique identifier for the asset (e.g., "BTC-USD")
     * @return List of historical prices with timestamps
     * @throws 404 Not Found if the symbol doesn't exist
     */
    @Operation(
        summary = "Get price history",
        description = "Returns historical price data for a specified asset"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved price history",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = AssetPrice.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Asset not found",
            content = @Content(schema = @Schema(hidden = true))
        )
    })
    @GetMapping("/history/{symbol}")
    public ResponseEntity<List<AssetPrice>> getPriceHistory(
            @Parameter(description = "Asset symbol, e.g. BTC-USD")
            @PathVariable String symbol) {
        try {
            return ResponseEntity.ok(marketSimulator.getPriceHistory(symbol));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Place a new order
     * 
     * @param request Order details including user ID, asset symbol, side (buy/sell),
     *                quantity, price, and order type (market/limit)
     * @return OrderResult containing success status, order ID, and message
     * @throws 400 Bad Request if order parameters are invalid
     * @throws 403 Forbidden if the order is rejected due to high risk
     */
    @Operation(
        summary = "Place a trading order",
        description = "Creates a new buy or sell order for a specified asset"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Order processed successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = TradingService.OrderResult.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid order parameters",
            content = @Content(schema = @Schema(hidden = true))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Order rejected due to high risk",
            content = @Content(schema = @Schema(hidden = true))
        )
    })
    @PostMapping("/order")
    public ResponseEntity<TradingService.OrderResult> placeOrder(
            @Parameter(description = "Order details") 
            @RequestBody OrderRequestDTO request) {
        TradingService.OrderResult result = tradingService.placeOrder(
            request.getUserId(),
            request.getSymbol(),
            request.getSide(),
            request.getQuantity(),
            request.getPrice(),
            request.getType()
        );
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Get open orders for a user
     * 
     * @param userId The unique identifier for the user
     * @return List of the user's open orders
     */
    @Operation(
        summary = "Get user's open orders",
        description = "Returns all open orders for a specified user"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved orders",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Order.class)
            )
        )
    })
    @GetMapping("/orders/{userId}")
    public ResponseEntity<List<Order>> getUserOrders(
            @Parameter(description = "User ID") 
            @PathVariable String userId) {
        return ResponseEntity.ok(tradingService.getUserOpenOrders(userId));
    }
    
    /**
     * Cancel an order
     * 
     * @param userId The unique identifier for the user
     * @param orderId The unique identifier for the order to cancel
     * @return OrderResult containing success status and message
     * @throws 404 Not Found if the order doesn't exist
     * @throws 403 Forbidden if the user doesn't own the order
     */
    @Operation(
        summary = "Cancel an order",
        description = "Cancels an existing open order"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Order cancelled successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = TradingService.OrderResult.class)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "User doesn't own this order",
            content = @Content(schema = @Schema(hidden = true))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Order not found",
            content = @Content(schema = @Schema(hidden = true))
        )
    })
    @DeleteMapping("/order/{userId}/{orderId}")
    public ResponseEntity<TradingService.OrderResult> cancelOrder(
            @Parameter(description = "User ID") @PathVariable String userId, 
            @Parameter(description = "Order ID to cancel") @PathVariable String orderId) {
        TradingService.OrderResult result = tradingService.cancelOrder(userId, orderId);
        return ResponseEntity.ok(result);
    }
    
    /**
     * Get user portfolio
     * 
     * @param userId The unique identifier for the user
     * @return Portfolio containing cash balance and asset holdings
     */
    @Operation(
        summary = "Get user portfolio",
        description = "Returns portfolio information including cash balance and asset holdings"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved portfolio",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Portfolio.class)
            )
        )
    })
    @GetMapping("/portfolio/{userId}")
    public ResponseEntity<Portfolio> getPortfolio(
            @Parameter(description = "User ID") 
            @PathVariable String userId) {
        return ResponseEntity.ok(portfolioService.getPortfolio(userId));
    }
    
    /**
     * Get portfolio total value (cash + assets at current market prices)
     * 
     * @param userId The unique identifier for the user
     * @return Total portfolio value in USD
     */
    @Operation(
        summary = "Get portfolio value",
        description = "Calculates total portfolio value including cash and assets at current market prices"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successfully calculated portfolio value",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(type = "number", format = "double")
            )
        )
    })
    @GetMapping("/portfolio/{userId}/value")
    public ResponseEntity<Double> getPortfolioValue(
            @Parameter(description = "User ID") 
            @PathVariable String userId) {
        return ResponseEntity.ok(portfolioService.getPortfolioValue(userId));
    }
    
    /**
     * Deposit funds to portfolio
     * 
     * @param userId The unique identifier for the user
     * @param request Object containing the amount to deposit
     * @return 200 OK if deposit successful
     * @throws 400 Bad Request if amount is invalid (negative or zero)
     */
    @Operation(
        summary = "Deposit funds",
        description = "Adds funds to the user's cash balance"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Deposit successful"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid amount (negative or zero)",
            content = @Content(
                mediaType = MediaType.TEXT_PLAIN_VALUE,
                schema = @Schema(type = "string")
            )
        )
    })
    @PostMapping("/portfolio/{userId}/deposit")
    public ResponseEntity<?> depositFunds(
            @Parameter(description = "User ID") @PathVariable String userId, 
            @Parameter(description = "Amount to deposit") @RequestBody FundsRequestDTO request) {
        try {
            portfolioService.depositFunds(userId, request.getAmount());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    /**
     * Withdraw funds from portfolio
     * 
     * @param userId The unique identifier for the user
     * @param request Object containing the amount to withdraw
     * @return 200 OK if withdrawal successful
     * @throws 400 Bad Request if amount is invalid or exceeds available balance
     */
    @Operation(
        summary = "Withdraw funds",
        description = "Withdraws funds from the user's cash balance"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Withdrawal successful"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid amount or insufficient funds",
            content = @Content(
                mediaType = MediaType.TEXT_PLAIN_VALUE,
                schema = @Schema(type = "string")
            )
        )
    })
    @PostMapping("/portfolio/{userId}/withdraw")
    public ResponseEntity<?> withdrawFunds(
            @Parameter(description = "User ID") @PathVariable String userId, 
            @Parameter(description = "Amount to withdraw") @RequestBody FundsRequestDTO request) {
        try {
            boolean success = portfolioService.withdrawFunds(userId, request.getAmount());
            if (success) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.badRequest().body("Insufficient funds");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}