package com.certificateverification.controller;

import com.certificateverification.blockchain.Blockchain;
import com.certificateverification.blockchain.HashUtil;
import com.certificateverification.model.Block;
import com.certificateverification.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for blockchain operations.
 * Day 3: Custom Java blockchain endpoints for adding blocks, viewing chain, and validation.
 * Day 7: Integrated audit logging for blockchain ledger validation events.
 */
@RestController
@RequestMapping("/api/blockchain")
public class BlockchainController {

    private final Blockchain blockchain;
    private final AuditLogService auditLogService;

    public BlockchainController(Blockchain blockchain) {
        this(blockchain, null);
    }

    @Autowired
    public BlockchainController(Blockchain blockchain, @Autowired(required = false) AuditLogService auditLogService) {
        this.blockchain = blockchain;
        this.auditLogService = auditLogService;
    }

    /**
     * Get all blocks in the blockchain.
     *
     * @return 200 OK with list of all blocks
     */
    @GetMapping
    public ResponseEntity<List<Block>> getBlockchain() {
        return ResponseEntity.ok(blockchain.getChain());
    }

    /**
     * Validate the complete blockchain and detect tampering.
     *
     * @return 200 OK with validation status ("Blockchain Valid" or "Blockchain Tampered")
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateBlockchain(HttpServletRequest request) {
        boolean isValid = blockchain.isChainValid();
        String ipAddress = request != null ? request.getRemoteAddr() : "127.0.0.1";

        if (auditLogService != null) {
            auditLogService.log(
                    "BLOCKCHAIN_LEDGER",
                    "BLOCKCHAIN_VALIDATION",
                    isValid ? "VALID" : "TAMPERED",
                    "Blockchain validation executed for " + blockchain.getChainSize() + " block(s). Result: " + (isValid ? "VALID" : "TAMPERED"),
                    ipAddress
            );
        }

        Map<String, Object> response = new HashMap<>();
        response.put("valid", isValid);
        response.put("status", isValid ? "Blockchain Valid" : "Blockchain Tampered");
        response.put("message", isValid ? "Blockchain Valid" : "Blockchain Tampered");
        response.put("totalBlocks", blockchain.getChainSize());
        return ResponseEntity.ok(response);
    }

    /**
     * Add a new certificate block to the blockchain.
     *
     * @param payload Map containing certificateId and certificateHash (or data)
     * @return 201 Created with created block, or 400 Bad Request
     */
    @PostMapping("/add")
    public ResponseEntity<?> addBlock(@RequestBody Map<String, String> payload) {
        try {
            String certificateId = payload.get("certificateId");
            String certificateHash = payload.get("certificateHash");

            // If raw data is provided instead of hash, compute hash
            if ((certificateHash == null || certificateHash.trim().isEmpty()) && payload.containsKey("data")) {
                certificateHash = HashUtil.sha256(payload.get("data"));
            }

            if (certificateId == null || certificateId.trim().isEmpty()) {
                certificateId = "BLOCK-" + System.currentTimeMillis();
            }

            if (certificateHash == null || certificateHash.trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Either 'certificateHash' or 'data' is required to add a block");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            Block newBlock = blockchain.addCertificateBlock(certificateId, certificateHash);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Block added to blockchain successfully.");
            response.put("block", newBlock);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "An error occurred adding block: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
