/*******************************************************************************
 * This file is part of the Incubed project.
 * Sources: https://github.com/slockit/in3-c
 * 
 * Copyright (C) 2018-2019 slock.it GmbH, Blockchains LLC
 * 
 * 
 * COMMERCIAL LICENSE USAGE
 * 
 * Licensees holding a valid commercial license may use this file in accordance 
 * with the commercial license agreement provided with the Software or, alternatively, 
 * in accordance with the terms contained in a written agreement between you and 
 * slock.it GmbH/Blockchains LLC. For licensing terms and conditions or further 
 * information please contact slock.it at in3@slock.it.
 * 	
 * Alternatively, this file may be used under the AGPL license as follows:
 *    
 * AGPL LICENSE USAGE
 * 
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free Software 
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 * [Permissions of this strong copyleft license are conditioned on making available 
 * complete source code of licensed works and modifications, which include larger 
 * works using a licensed work, under the same license. Copyright and license notices 
 * must be preserved. Contributors provide an express grant of patent rights.]
 * You should have received a copy of the GNU Affero General Public License along 
 * with this program. If not, see <https://www.gnu.org/licenses/>.
 *******************************************************************************/

package in3.eth1;

import in3.*;
import in3.eth1.*;

import java.math.BigInteger;

/**
 * a Wrapper for the incubed client offering Type-safe Access and additional
 * helper functions.
 */
public class API {

    private IN3 in3;

    /**
     * creates a API using the given incubed instance.
     */
    public API(IN3 in3) {
        this.in3 = in3;
    }

    /**
     * finds the Block as specified by the number. use `Block.LATEST` for getting
     * the lastest block.
     */
    public Block getBlockByNumber(long block, /** < the Blocknumber */
    boolean includeTransactions /**
                                 * < if true all Transactions will be includes, if not only the
                                 * transactionhashes
                                 */
    ) {
        return Block.asBlock(in3.sendRPCasObject("eth_getBlockByNumber",
                new Object[] { getBlockString(block), includeTransactions }));
    }

    /**
     * Returns information about a block by hash.
     */
    public Block getBlockByHash(String blockHash, /** < the Blocknumber */
    boolean includeTransactions /**
                                 * < if true all Transactions will be includes, if not only the
                                 * transactionhashes
                                 */
    ) {
        return Block
                .asBlock(in3.sendRPCasObject("eth_getBlockByHash", new Object[] { blockHash, includeTransactions }));
    }

    /**
     * the current BlockNumber.
     */
    public long getBlockNumber() {
        return JSON.asLong(in3.sendRPCasObject("eth_blockNumber", new Object[] {}));
    }

    /**
     * the current Gas Price.
     */
    public long getGasPrice() {
        return JSON.asLong(in3.sendRPCasObject("eth_gasPrice", new Object[] {}));
    }

    /**
     * Returns the EIP155 chain ID used for transaction signing at the current best
     * block. Null is returned if not available.
     */
    public String getChainId() {
        return JSON.asString(in3.sendRPCasObject("eth_chainId", new Object[] {}));
    }

    /**
     * calls a function of a smart contract and returns the result.
     * 
     * @return the decoded result. if only one return value is expected the Object
     *         will be returned, if not an array of objects will be the result.
     */
    public Object call(TransactionRequest request, /** < the transaction to call. */
    long block /** < the Block used to for the state. */
    ) {
        return request.getResult((String) in3.sendRPCasObject("eth_call",
                new Object[] { request.getTransactionJson(), getBlockString(block) }));
    }

    /**
     * Makes a call or transaction, which won't be added to the blockchain and
     * returns the used gas, which can be used for estimating the used gas.
     * 
     * @return the gas required to call the function.
     */
    public long estimateGas(TransactionRequest request, /** < the transaction to call. */
    long block /** < the Block used to for the state. */
    ) {
        return JSON.asLong(in3.sendRPCasObject("eth_estimateGas",
                new Object[] { request.getTransactionJson(), getBlockString(block) }));
    }

    /**
     * Returns the balance of the account of given address in wei.
     */
    public BigInteger getBalance(String address, long block) {
        return JSON
                .asBigInteger(in3.sendRPCasObject("eth_getBalance", new Object[] { address, getBlockString(block) }));
    }

    /**
     * Returns code at a given address.
     */
    public String getCode(String address, long block) {
        return JSON.asString(in3.sendRPCasObject("eth_getCode", new Object[] { address, getBlockString(block) }));
    }

    /**
     * Returns the value from a storage position at a given address.
     */
    public String getStorageAt(String address, BigInteger position, long block) {
        return JSON.asString(in3.sendRPCasObject("eth_getStorageAt",
                new Object[] { address, JSON.asString(position), getBlockString(block) }));
    }

    /**
     * Returns the number of transactions in a block from a block matching the given
     * block hash.
     */
    public long getBlockTransactionCountByHash(String blockHash) {
        return JSON.asLong(in3.sendRPCasObject("eth_getBlockTransactionCountByHash", new Object[] { blockHash }));
    }

    /**
     * Returns the number of transactions in a block from a block matching the given
     * block number.
     */
    public long getBlockTransactionCountByNumber(long block) {
        return JSON.asLong(
                in3.sendRPCasObject("eth_getBlockTransactionCountByNumber", new Object[] { getBlockString(block) }));
    }

    /**
     * Polling method for a filter, which returns an array of logs which occurred
     * since last poll.
     */
    public Log[] getFilterChangesFromLogs(long id) {
        return Log.asLogs(in3.sendRPCasObject("eth_getFilterChanges", new Object[] { JSON.asString(id) }));
    }

    /**
     * Polling method for a filter, which returns an array of logs which occurred
     * since last poll.
     */
    public String[] getFilterChangesFromBlocks(long id) {
        return JSON.asStringArray(in3.sendRPCasObject("eth_getFilterChanges", new Object[] { JSON.asString(id) }));
    }

    /**
     * Polling method for a filter, which returns an array of logs which occurred
     * since last poll.
     */
    public Log[] getFilterLogs(long id) {
        return Log.asLogs(in3.sendRPCasObject("eth_getFilterLogs", new Object[] { JSON.asString(id) }));
    }

    /**
     * Polling method for a filter, which returns an array of logs which occurred
     * since last poll.
     */
    public Log[] getLogs(LogFilter filter) {
        return Log.asLogs(in3.sendRPCasObject("eth_getLogs", new Object[] { filter.toString() }));
    }

    /**
     * Returns information about a transaction by block hash and transaction index
     * position.
     */
    public Transaction getTransactionByBlockHashAndIndex(String blockHash, int index) {
        return Transaction.asTransaction(in3.sendRPCasObject("eth_getTransactionByBlockHashAndIndex",
                new Object[] { blockHash, JSON.asString(index) }));
    }

    /**
     * Returns information about a transaction by block number and transaction index
     * position.
     */
    public Transaction getTransactionByBlockNumberAndIndex(long block, int index) {
        return Transaction.asTransaction(in3.sendRPCasObject("eth_getTransactionByBlockNumberAndIndex",
                new Object[] { JSON.asString(block), JSON.asString(index) }));
    }

    /**
     * Returns the information about a transaction requested by transaction hash.
     */
    public Transaction getTransactionByHash(String transactionHash) {
        return Transaction
                .asTransaction(in3.sendRPCasObject("eth_getTransactionByHash", new Object[] { transactionHash }));
    }

    /**
     * Returns the number of transactions sent from an address.
     */
    public BigInteger getTransactionCount(String address, long block) {
        return JSON.asBigInteger(
                in3.sendRPCasObject("eth_getTransactionCount", new Object[] { address, getBlockString(block) }));
    }

    /**
     * Returns the number of transactions sent from an address.
     */
    public TransactionReceipt getTransactionReceipt(String transactionHash) {
        return TransactionReceipt.asTransactionReceipt(
                in3.sendRPCasObject("eth_getTransactionReceipt", new Object[] { transactionHash }));
    }

    /**
     * Returns information about a uncle of a block number and uncle index position.
     * Note: An uncle doesn't contain individual transactions.
     */
    public Block getUncleByBlockNumberAndIndex(long block, int pos) {
        return Block.asBlock(in3.sendRPCasObject("eth_getUncleByBlockNumberAndIndex",
                new Object[] { getBlockString(block), JSON.asString(pos) }));
    }

    /**
     * Returns the number of uncles in a block from a block matching the given block
     * hash.
     */
    public long getUncleCountByBlockHash(String block) {
        return JSON.asLong(in3.sendRPCasObject("eth_getUncleCountByBlockHash", new Object[] { block }));
    }

    /**
     * Returns the number of uncles in a block from a block matching the given block
     * hash.
     */
    public long getUncleCountByBlockNumber(long block) {
        return JSON
                .asLong(in3.sendRPCasObject("eth_getUncleCountByBlockNumber", new Object[] { getBlockString(block) }));
    }

    /**
     * Creates a filter in the node, to notify when a new block arrives. To check if
     * the state has changed, call eth_getFilterChanges.
     */
    public long newBlockFilter() {
        return JSON.asLong(in3.sendRPCasObject("eth_newBlockFilter", new Object[] {}));
    }

    /**
     * Creates a filter object, based on filter options, to notify when the state
     * changes (logs). To check if the state has changed, call eth_getFilterChanges.
     * 
     * A note on specifying topic filters: Topics are order-dependent. A transaction
     * with a log with topics [A, B] will be matched by the following topic filters:
     * 
     * [] "anything" [A] "A in first position (and anything after)" [null, B]
     * "anything in first position AND B in second position (and anything after)"
     * [A, B] "A in first position AND B in second position (and anything after)"
     * [[A, B], [A, B]] "(A OR B) in first position AND (A OR B) in second position
     * (and anything after)"
     * 
     */
    public long newLogFilter(LogFilter filter) {
        return JSON.asLong(in3.sendRPCasObject("eth_newFilter", new Object[] { filter.toString() }));
    }

    /**
     * uninstall filter.
     */
    public boolean uninstallFilter(long filter) {
        return (boolean) in3.sendRPCasObject("eth_uninstallFilter", new Object[] { JSON.asString(filter) });
    }

    /**
     * Creates new message call transaction or a contract creation for signed
     * transactions.
     * 
     * @return transactionHash
     */
    public String sendRawTransaction(String data) {
        return JSON.asString(in3.sendRPCasObject("eth_sendRawTransaction", new Object[] { data }));
    }

    /**
     * sends a Transaction as desribed by the TransactionRequest. This will require
     * a signer to be set in order to sign the transaction.
     */
    public String sendTransaction(TransactionRequest tx) {
        Signer signer = in3.getSigner();
        if (signer == null)
            throw new RuntimeException("No Signer set. This is needed in order to sign transaction.");
        if (tx.getFrom() == null)
            throw new RuntimeException("No from address set");
        if (!signer.hasAccount(tx.getFrom()))
            throw new RuntimeException("The from address is not supported by the signer");
        tx = signer.prepareTransaction(in3, tx);

        return JSON.asString(in3.sendRPCasObject("eth_sendTransaction", new Object[] { tx.getTransactionJson() }));
    }

    /**
     * the current Gas Price.
     * 
     * @return the decoded result. if only one return value is expected the Object
     *         will be returned, if not an array of objects will be the result.
     */
    public Object call(String to, String function, Object... params) {
        TransactionRequest req = new TransactionRequest();
        req.setTo(to);
        req.setFunction(function);
        req.setParams(params);
        return call(req, Block.LATEST);
    }

    private static String getBlockString(long l) {
        return l == Block.LATEST ? "latest" : "0x" + Long.toHexString(l);
    }

}
