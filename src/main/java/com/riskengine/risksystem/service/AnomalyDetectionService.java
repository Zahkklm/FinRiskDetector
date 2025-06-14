package com.riskengine.risksystem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.riskengine.risksystem.model.Transaction;
import com.riskengine.risksystem.repository.TransactionRepository;
import com.riskengine.risksystem.util.MLModelUtil;

import java.util.List;

@Service
public class AnomalyDetectionService {

    private final TransactionRepository transactionRepository;
    private final MLModelUtil mlModelUtil;

    @Autowired
    public AnomalyDetectionService(TransactionRepository transactionRepository, MLModelUtil mlModelUtil) {
        this.transactionRepository = transactionRepository;
        this.mlModelUtil = mlModelUtil;
    }

    public List<Transaction> detectAnomalies(Pageable pageable) {
        Page<Transaction> transactions = transactionRepository.findAll(pageable);
        return mlModelUtil.detectAnomalies(transactions.getContent());
    }

    public void logAnomaly(Transaction transaction) {
        // Logic to log the detected anomaly
        // This could involve saving to a database or sending a notification
    }
}