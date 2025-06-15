package com.riskengine.risksystem.controller;

import com.riskengine.risksystem.dto.TransactionDTO;
import com.riskengine.risksystem.model.Transaction;
import com.riskengine.risksystem.service.TransactionProcessingService;

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

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for transaction monitoring and management operations
 * Provides endpoints for tracking and processing financial transactions
 */
@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transaction Monitor", description = "API for transaction tracking and management")
public class TransactionMonitorController {

    private final TransactionProcessingService transactionProcessingService;

    @Autowired
    public TransactionMonitorController(TransactionProcessingService transactionProcessingService) {
        this.transactionProcessingService = transactionProcessingService;
    }

    /**
     * Get all transactions in the system
     * 
     * @return List of all transactions converted to DTOs
     */
    @Operation(
        summary = "Get all transactions",
        description = "Retrieves a list of all transactions in the system"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved transaction list",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = TransactionDTO.class)
            )
        )
    })
    @GetMapping
    public ResponseEntity<List<TransactionDTO>> getAllTransactions() {
        List<Transaction> transactions = transactionProcessingService.getAllTransactions();
        List<TransactionDTO> transactionDTOs = transactions.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(transactionDTOs);
    }

    /**
     * Get a specific transaction by ID
     * 
     * @param id The unique identifier of the transaction
     * @return Transaction details as DTO
     * @throws EntityNotFoundException if transaction not found
     */
    @Operation(
        summary = "Get transaction by ID",
        description = "Retrieves detailed information about a specific transaction"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved transaction",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = TransactionDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Transaction not found",
            content = @Content(schema = @Schema(hidden = true))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<TransactionDTO> getTransactionById(
            @Parameter(description = "Transaction ID") 
            @PathVariable Long id) {
        try {
            Transaction transaction = transactionProcessingService.getTransactionById(id);
            if (transaction == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(convertToDTO(transaction));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Create and process a new transaction
     * 
     * @param transactionDTO The transaction details to process
     * @return Processed transaction with updated risk information
     * @throws IllegalArgumentException if transaction validation fails
     */
    @Operation(
        summary = "Create new transaction",
        description = "Processes and stores a new financial transaction with risk assessment"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Transaction successfully processed",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = TransactionDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid transaction data",
            content = @Content(schema = @Schema(hidden = true))
        )
    })
    @PostMapping
    public ResponseEntity<TransactionDTO> createTransaction(
            @Parameter(description = "Transaction details to process") 
            @RequestBody TransactionDTO transactionDTO) {
        Transaction transaction = convertToEntity(transactionDTO);
        transactionProcessingService.processTransaction(transaction);
        return ResponseEntity.ok(convertToDTO(transaction));
    }

    /**
     * Delete a transaction from the system
     * 
     * @param id The unique identifier of the transaction to delete
     * @return Empty response with 204 No Content status
     * @throws EntityNotFoundException if transaction not found
     */
    @Operation(
        summary = "Delete transaction",
        description = "Removes a transaction from the system"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "204",
            description = "Transaction successfully deleted",
            content = @Content(schema = @Schema(hidden = true))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Transaction not found",
            content = @Content(schema = @Schema(hidden = true))
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(
            @Parameter(description = "Transaction ID to delete") 
            @PathVariable Long id) {
        transactionProcessingService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Convert Transaction entity to TransactionDTO
     */
    private TransactionDTO convertToDTO(Transaction transaction) {
        return TransactionDTO.builder()
            .id(transaction.getId())
            .userId(transaction.getUserId())
            .amount(transaction.getAmount())
            .timestamp(transaction.getTimestamp())
            .type(transaction.getType())
            .sourceAccountId(transaction.getSourceAccountId())
            .destinationAccountId(transaction.getDestinationAccountId())
            .riskScore(transaction.getRiskScore())
            // Assuming you have a way to get risk level from risk score
            // This may need adjustment based on your actual model
            .build();
    }
    
    /**
     * Convert TransactionDTO to Transaction entity
     */
    private Transaction convertToEntity(TransactionDTO dto) {
        return Transaction.builder()
            .id(dto.getId())
            .userId(dto.getUserId())
            .amount(dto.getAmount())
            .timestamp(dto.getTimestamp() != null ? dto.getTimestamp() : java.time.LocalDateTime.now())
            .type(dto.getType())
            .sourceAccountId(dto.getSourceAccountId())
            .destinationAccountId(dto.getDestinationAccountId())
            .riskScore(dto.getRiskScore())
            .build();
    }
}