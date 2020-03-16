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

/**
 * Log configuration for search logs.
 */

public class LogFilter {
  /**
     * Quantity or Tag - (optional) (default: latest) Integer block number, or
     * 'latest' for the last mined block or 'pending', 'earliest' for not yet mined
     * transactions.
     */
  private long fromBlock = Block.LATEST;
  /**
     * Quantity or Tag - (optional) (default: latest) Integer block number, or
     * 'latest' for the last mined block or 'pending', 'earliest' for not yet mined
     * transactions.
     */
  private long toBlock = Block.LATEST;
  /**
     * (optional) 20 Bytes - Contract address or a list of addresses from which logs
     * should originate.
     */
  private String address;
  /**
     * (optional) Array of 32 Bytes Data topics. Topics are order-dependent. It's
     * possible to pass in null to match any topic, or a subarray of multiple topics
     * of which one should be matching.
     */
  private Object[] topics;

  /** a(optional) The maximum number of entries to retrieve (latest first). */
  private int limit;

  public long getFromBlock() {
    return fromBlock;
  }

  public void setFromBlock(long fromBlock) {
    this.fromBlock = fromBlock;
  }

  public long getToBlock() {
    return toBlock;
  }

  public void setToBlock(long toBlock) {
    this.toBlock = toBlock;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public Object[] getTopics() {
    return topics;
  }

  public void setTopics(Object[] topics) {
    this.topics = topics;
  }

  public int getLimit() {
    return limit;
  }

  public void setLimit(int limit) {
    this.limit = limit;
  }

  /**
     * creates a JSON-String.
     */
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    if (fromBlock >= 0)
      JSON.appendKey(sb, "fromBlock", "0x" + Long.toHexString(fromBlock));
    if (toBlock >= 0)
      JSON.appendKey(sb, "toBlock", "0x" + Long.toHexString(toBlock));
    if (topics != null)
      JSON.appendKey(sb, "topics", JSON.toJson(topics));
    if (limit > 0)
      JSON.appendKey(sb, "limit", JSON.asString(limit));
    if (address != null)
      JSON.appendKey(sb, "address", JSON.asString(address));
    sb.setCharAt(sb.length() - 1, '}');

    return sb.toString();
  }
}