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

import java.math.BigInteger;

import in3.Loader;
import in3.utils.JSON;

/**
 * represents a Transaction Request which should be send or called.
 */

public class TransactionRequest {
    /**
     * the from address
     */
    private String from;

    /**
     * the recipients address
     */
    private String to;

    /**
     * the data
     */
    private String data;

    /**
     * the value of the transaction
     */
    private BigInteger value;

    /**
     * the nonce (transactionCount of the sender)
     */
    private long nonce = -1;

    /**
     * the gas to use
     */
    private long gas;

    /**
     * the gas price to use
     */
    private long gasPrice;

    /**
     * the signature for the function to call
     */
    private String function;

    /**
     * the params to use for encoding in the data
     */
    private Object[] params;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public BigInteger getValue() {
        return value;
    }

    public void setValue(BigInteger value) {
        this.value = value;
    }

    public long getNonce() {
        return nonce;
    }

    public void setNonce(long nonce) {
        this.nonce = nonce;
    }

    public long getGas() {
        return gas;
    }

    public void setGas(long gas) {
        this.gas = gas;
    }

    public long getGasPrice() {
        return gasPrice;
    }

    public void setGasPrice(long gasPrice) {
        this.gasPrice = gasPrice;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public Object[] getParams() {
        return params;
    }

    public void setParams(Object[] params) {
        this.params = params;
    }

    public void setData(String data) {
        this.data = data;
    }

    /**
     * creates the data based on the function/params values.
     */
    public String getData() {
        String result = data == null || data.length() < 2 ? "0x" : data;
        if (function != null) {
            String fnData = abiEncode(function, JSON.toJson(params));
            if (fnData != null && fnData.length() > 2 && fnData.startsWith("0x"))
                result += fnData.substring(2 + (result.length() > 2 ? 8 : 0));
        }
        return result;
    }

    public String getTransactionJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (to != null)
            JSON.appendKey(sb, "to", to);
        if (from != null)
            JSON.appendKey(sb, "from", from);
        if (gas > 0)
            JSON.appendKey(sb, "gas", JSON.asString(gas));
        if (gasPrice > 0)
            JSON.appendKey(sb, "gasPrice", JSON.asString(gasPrice));
        if (value != null)
            JSON.appendKey(sb, "value", JSON.asString(value));
        if (nonce >= 0)
            JSON.appendKey(sb, "nonce", JSON.asString(nonce));
        JSON.appendKey(sb, "data", getData());
        sb.setCharAt(sb.length() - 1, '}');
        return sb.toString();
    }

    public Object getResult(String data) {
        if (function == null)
            return data;
        Object[] res = (Object[]) abiDecode(function, data);
        return res.length == 1 ? res[0] : res;
    }

    static {
        Loader.loadLibrary();
    }

    private static native String abiEncode(String function, String jsonParams);

    private static native Object abiDecode(String function, String data);
}