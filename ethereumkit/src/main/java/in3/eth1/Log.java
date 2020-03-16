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
 * a log entry of a transaction receipt.
 * 
 */

public class Log {

  private JSON data;

  private Log(JSON data) {
    this.data = data;
  }

  protected static Log[] asLogs(Object o) {
    if (o == null)
      return null;
    if (o instanceof Object[]) {
      Object[] a = (Object[]) o;
      Log[] s    = new Log[a.length];
      for (int i = 0; i < s.length; i++)
        s[i] = a[i] == null ? null : new Log((JSON) a[i]);
      return s;
    }
    return null;
  }

  protected static Log asLog(Object o) {
    if (o == null)
      return null;
    return new Log((JSON) o);
  }

  /**
     * true when the log was removed, due to a chain reorganization. false if its a
     * valid log.
     */
  public boolean isRemoved() {
    return (Boolean) data.get("removed");
  }

  /**
     * integer of the log index position in the block. null when its pending log.
     */
  public int getLogIndex() {
    return JSON.asInt(data.get("logIndex"));
  }

  /**
     * integer of the transactions index position log was created from. null when
     * its pending log.
     */
  public int gettTansactionIndex() {
    return JSON.asInt(data.get("transactionIndex"));
  }

  /**
     * Hash, 32 Bytes - hash of the transactions this log was created from. null
     * when its pending log.
     */
  public String getTransactionHash() {
    return data.getString("transactionHash");
  }

  /**
     * Hash, 32 Bytes - hash of the block where this log was in. null when its
     * pending. null when its pending log.
     */
  public String getBlockHash() {
    return data.getString("blockHash");
  }

  /**
     * the block number where this log was in. null when its pending. null when its
     * pending log.
     */
  public long getBlockNumber() {
    return data.getLong("blockNumber");
  }

  /**
     * 20 Bytes - address from which this log originated.
     */
  public String getAddress() {
    return data.getString("address");
  }

  /**
     * Array of 0 to 4 32 Bytes DATA of indexed log arguments. (In solidity: The
     * first topic is the hash of the signature of the event (e.g.
     * Deposit(address,bytes32,uint256)), except you declared the event with the
     * anonymous specifier.)
     */
  public String[] getTopics() {
    return data.getStringArray("topics");
  }
}
