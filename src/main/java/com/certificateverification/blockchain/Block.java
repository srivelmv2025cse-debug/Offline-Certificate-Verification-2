package com.certificateverification.blockchain;

import lombok.NoArgsConstructor;

/**
 * Domain Block representation within the blockchain package,
 * extending the persistent entity model.
 */
@NoArgsConstructor
public class Block extends com.certificateverification.model.Block {

    public Block(int index, long timestamp, String certificateId, String certificateHash, String previousHash, String hash) {
        super(index, timestamp, certificateId, certificateHash, previousHash, hash);
    }
}
