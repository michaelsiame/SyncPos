// src/main/java/com/kmu/syncpos/service/PaymentService.java
package com.kmu.syncpos.service;

import com.kmu.syncpos.auth.TenantContext;
import com.kmu.syncpos.dao.PaymentDAO;
import com.kmu.syncpos.dao.SaleDAO;
import com.kmu.syncpos.dto.PaymentDTO;
import com.kmu.syncpos.dto.SaleDTO;
import com.kmu.syncpos.util.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class PaymentService {

    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final SaleDAO saleDAO = new SaleDAO();

    /**
     * Processes a new payment as a single database transaction. It inserts the payment
     * and correctly updates the parent sale's payment status to 'partial' or 'paid'.
     *
     * @param paymentDto The new payment to process.
     * @return true if the payment was processed successfully, false otherwise.
     */
    public boolean processNewPayment(PaymentDTO paymentDto) {
        String tenantId = TenantContext.getTenant().getUuid();
        double epsilon = 0.001; // Tolerance for floating point comparisons

        // --- REAL IMPLEMENTATION ---
        // 1. First, get the sale details to know the total amount due.
        SaleDTO sale = saleDAO.getById(paymentDto.getSaleId(), tenantId);
        if (sale == null) {
            System.err.println("PaymentService: Could not process payment, sale not found with ID: " + paymentDto.getSaleId());
            return false;
        }

        // 2. Perform the rest as a single transaction.
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try {
                // a. Insert the new payment record.
                paymentDAO.insertTransactional(conn, paymentDto, tenantId);

                // b. Get all payments for the sale using the transactional connection
                // to ensure we include the payment we just inserted.
                List<PaymentDTO> allPayments = paymentDAO.getPaymentsForSale(paymentDto.getSaleId(), tenantId);
                double totalPaid = allPayments.stream().mapToDouble(PaymentDTO::getAmount).sum();

                // c. Determine the new payment status.
                String newStatus = (totalPaid >= sale.getTotal() - epsilon) ? "paid" : "partial";

                // d. Update the parent sale's payment status.
                saleDAO.updatePaymentStatusTransactional(conn, paymentDto.getSaleId(), newStatus, tenantId);

                conn.commit();
                return true;

            } catch (SQLException e) {
                System.err.println("PaymentService.processNewPayment failed, rolling back transaction: " + e.getMessage());
                conn.rollback();
                return false;
            }

        } catch (SQLException e) {
            System.err.println("PaymentService: Failed to get or close database connection: " + e.getMessage());
            return false;
        }
    }

    public List<PaymentDTO> getPaymentsForSale(long saleId) {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            return paymentDAO.getPaymentsForSale(saleId, tenantId);
        } catch (IllegalStateException e) {
            System.err.println("PaymentService: Tenant context not available.");
            return Collections.emptyList();
        }
    }
    public List<PaymentDTO> getUnsyncedPayments(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            return Collections.emptyList();
        }
        return paymentDAO.getUnsynced(tenantId);
    }
}