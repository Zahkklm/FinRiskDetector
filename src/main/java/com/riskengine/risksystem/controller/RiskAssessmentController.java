package com.riskengine.risksystem.controller;

import com.riskengine.risksystem.dto.RiskScoreRequestDTO;
import com.riskengine.risksystem.dto.TransactionRiskRequestDTO;
import com.riskengine.risksystem.dto.RiskScoreResponseDTO;
import com.riskengine.risksystem.model.RiskScore;
import com.riskengine.risksystem.model.Transaction;
import com.riskengine.risksystem.repository.TransactionRepository;
import com.riskengine.risksystem.service.RiskScoringService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityNotFoundException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Controller for risk assessment operations
 * Provides endpoints for evaluating transaction risks
 */
@RestController
@RequestMapping("/api/risk-assessment")
@Tag(name = "Risk Assessment", description = "API for financial transaction risk evaluation")
public class RiskAssessmentController {

    private final RiskScoringService riskScoringService;
    private final TransactionRepository transactionRepository;

    @Autowired
    public RiskAssessmentController(RiskScoringService riskScoringService, 
                                   TransactionRepository transactionRepository) {
        this.riskScoringService = riskScoringService;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Evaluate risk for an existing transaction
     *
     * @param request DTO containing the transaction ID to evaluate
     * @return RiskScoreResponseDTO containing risk score and level information
     * @throws EntityNotFoundException if transaction not found
     */
    @Operation(
        summary = "Evaluate risk for an existing transaction",
        description = "Retrieves a transaction by ID and calculates its risk score and level"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Risk successfully evaluated",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = RiskScoreResponseDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Transaction not found",
            content = @Content(schema = @Schema(hidden = true))
        )
    })
    @PostMapping("/evaluate")
    public ResponseEntity<RiskScoreResponseDTO> evaluateRisk(
            @Parameter(description = "Transaction ID for risk evaluation")
            @RequestBody RiskScoreRequestDTO request) {
        
        // Find the transaction associated with this risk score
        Transaction transaction = transactionRepository.findById(request.getTransactionId())
            .orElseThrow(() -> new EntityNotFoundException("Transaction not found with id: " + 
                                                         request.getTransactionId()));
        
        // Create a proper risk score
        RiskScore evaluatedRiskScore = riskScoringService.createRiskScore(transaction);
        
        // Convert to response DTO
        RiskScoreResponseDTO response = RiskScoreResponseDTO.builder()
            .transactionId(evaluatedRiskScore.getTransactionId())
            .score(evaluatedRiskScore.getScore())
            .level(evaluatedRiskScore.getLevel())
            .createdAt(evaluatedRiskScore.getCreatedAt())
            .build();
            
        return ResponseEntity.ok(response);
    }

    /**
     * Evaluate risk for a transaction without storing it
     *
     * @param requestDTO DTO containing transaction details for risk evaluation
     * @return RiskScoreResponseDTO containing the calculated risk score and level
     */
    @Operation(
        summary = "Evaluate risk for a new transaction",
        description = "Evaluates risk for a transaction that hasn't been stored yet"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Risk successfully evaluated",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = RiskScoreResponseDTO.class)
            )
        )
    })
    @PostMapping("/evaluate-transaction")
    public ResponseEntity<RiskScoreResponseDTO> evaluateTransactionRisk(
            @Parameter(description = "Transaction details for risk evaluation")
            @RequestBody TransactionRiskRequestDTO requestDTO) {
        
        // Convert DTO to Transaction object
        Transaction transaction = Transaction.builder()
            .userId(requestDTO.getUserId())
            .amount(requestDTO.getAmount())
            .timestamp(requestDTO.getTimestamp() != null ? requestDTO.getTimestamp() : LocalDateTime.now())
            .type(requestDTO.getType())
            .sourceAccountId(requestDTO.getSourceAccountId())
            .destinationAccountId(requestDTO.getDestinationAccountId())
            .build();
        
        // Create risk score
        RiskScore evaluatedRiskScore = riskScoringService.createRiskScore(transaction);
        
        // Convert to response DTO
        RiskScoreResponseDTO response = RiskScoreResponseDTO.builder()
            .transactionId(evaluatedRiskScore.getTransactionId())
            .score(evaluatedRiskScore.getScore())
            .level(evaluatedRiskScore.getLevel())
            .createdAt(evaluatedRiskScore.getCreatedAt())
            .build();
            
        return ResponseEntity.ok(response);
    }

    /**
     * Check if the risk assessment service is operational
     *
     * @return String message indicating service status
     */
    @Operation(
        summary = "Get service status",
        description = "Check if the risk assessment service is up and running"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Service is operational",
            content = @Content(
                mediaType = MediaType.TEXT_PLAIN_VALUE,
                schema = @Schema(type = "string")
            )
        )
    })
    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok("Risk Assessment Service is running");
    }
}