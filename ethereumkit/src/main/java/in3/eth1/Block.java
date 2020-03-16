/*******************************************************************************
 * This file is part of the Incubed project.
 * Sources: https://github.com/slockit/in3-c
 * 
 * Copyright (C) 2018-2020 slock.it GmbH, Blockchains LLC
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

import in3.utils.JSON;
import java.math.*;

/**
 * represents a Block in ethereum.
 * 
 */

public class Block {

  /**
     * The latest Block Number.
     */
  public static long LATEST = -1;

  /**
     * The Genesis Block.
     */
  public static long EARLIEST = 0;

  private JSON data;

  private Block(JSON data) {
    this.data = data;
  }

  protected static Block asBlock(Object o) {
    if (o == null)
      return null;
    return new Block((JSON) o);
  }

  protected static Block[] asBlocks(Object o) {
    if (o == null)
      return null;
    Object[] a = (Object[]) o;
    Block[] b  = new Block[a.length];
    for (int i = 0; i < a.length; i++)
      b[i] = Block.asBlock(a[i]);
    return b;
  }

  /**
     * returns the total Difficulty as a sum of all difficulties starting from
     * genesis.
     */
  public BigInteger getTotalDifficulty() {
    return data.getBigInteger("totalDifficulty");
  }

  /**
     * the gas limit of the block.
     */
  public BigInteger getGasLimit() {
    return data.getBigInteger("gasLimit");
  }

  /**
     * the extra data of the block.
     */
  public String getExtraData() {
    return data.getString("extraData");
  }

  /**
     * the difficulty of the block.
     */
  public BigInteger getDifficulty() {
    return data.getBigInteger("difficulty");
  }

  /**
     * the author or miner of the block.
     */
  public String getAuthor() {
    return data.getString("author");
  }

  /**
     * the roothash of the merkletree containing all transaction of the block.
     */
  public String getTransactionsRoot() {
    return data.getString("transactionsRoot");
  }

  /**
     * the roothash of the merkletree containing all transaction receipts of the
     * block.
     */
  public String getTransactionReceiptsRoot() {
    return data.getString("receiptsRoot");
  }

  /**
     * the roothash of the merkletree containing the complete state.
     */
  public String getStateRoot() {
    return data.getString("stateRoot");
  }

  /**
     * the transaction hashes of the transactions in the block.
     */
  public String[] getTransactionHashes() {
    Object[] tx = (Object[]) data.get("transactions");
    if (tx == null || tx.length == 0)
      return new String[0];
    if (tx[0] instanceof String)
      return data.getStringArray("transactions");
    String[] res = new String[tx.length];
    for (int i = 0; i < tx.length; i++)
      res[i] = ((JSON) tx[i]).getString("hash");
    return res;
  }

  /**
     * the transactions of the block.
     * 
     * @throws if the Transactions are noot available
     */
  public Transaction[] getTransactions() throws Exception {
    Object[] tx = (Object[]) data.get("transactions");
    if (tx == null || tx.length == 0)
      return new Transaction[0];

    if (tx[0] instanceof String)
      throw new Exception("The Block only contains the transaction hashes!");

    Transaction[] res = new Transaction[tx.length];
    for (int i = 0; i < tx.length; i++)
      res[i] = Transaction.asTransaction(tx[i]);
    return res;
  }

  /**
     * the unix timestamp in seconds since 1970.
     */
  public long getTimeStamp() {
    return data.getLong("timestamp");
  }

  /**
     * the roothash of the merkletree containing all uncles of the block.
     */
  public String getSha3Uncles() {
    return data.getString("sha3Uncles");
  }

  /**
     * the size of the block.
     */
  public long getSize() {
    return data.getLong("size");
  }

  /**
     * the seal fields used for proof of authority.
     */
  public String[] getSealFields() {
    return data.getStringArray("sealFields");
  }

  /**
     * the block hash of the of the header.
     */
  public String getHash() {
    return data.getString("hash");
  }

  /**
     * the bloom filter of the block.
     */
  public String getLogsBloom() {
    return data.getString("logsBloom");
  }

  /**
     * the mix hash of the block. (only valid of proof of work)
     */
  public String getMixHash() {
    return data.getString("mixHash");
  }

  /**
     * the mix hash of the block. (only valid of proof of work)
     */
  public String getNonce() {
    return data.getString("nonce");
  }

  /**
     * the block number
     */
  public long getNumber() {
    return data.getLong("number");
  }

  /**
     * the hash of the parent-block.
     */
  public String getParentHash() {
    return data.getString("parentHash");
  }

  /**
     * returns the blockhashes of all uncles-blocks.
     */
  public String[] getUncles() {
    return data.getStringArray("uncles");
  }

  @Override
  public int hashCode() {
    final int prime  = 31;
    int       result = 1;
    result           = prime * result + ((data == null) ? 0 : data.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Block other = (Block) obj;
    if (data == null) {
      if (other.data != null)
        return false;
    } else if (!data.equals(other.data))
      return false;
    return true;
  }
}
