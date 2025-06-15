package com.riskengine.risksystem.dto;

import com.riskengine.risksystem.market.model.Order;
import lombok.Data;

/**
 * Data transfer object for order placement requests.
 * Contains all information needed to place a new market or limit order.
 */
@Data
public class OrderRequestDTO {
    private String userId;
    private String symbol;
    private Order.OrderSide side;
    private double quantity;
    private double price;
    private Order.OrderType type;
}