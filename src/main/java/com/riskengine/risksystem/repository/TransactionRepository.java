package com.riskengine.risksystem.repository;

import com.riskengine.risksystem.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    // Custom query methods can be defined here if needed
}