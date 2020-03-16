package in3.utils;

import in3.IN3;
import in3.Loader;

/**
 * a Wrapper for crypto-related helper functions.
 */
public class Crypto {

  static {
    Loader.loadLibrary();
  }

  private static final String PK2ADDRESS  = "in3_pk2address";
  private static final String PK2PUBLIC   = "in3_pk2public";
  private static final String ECRECOVER   = "in3_ecrecover";
  private static final String DECRYPT_KEY = "in3_decryptKey";
  private static final String SHA3        = "web3_sha3";
  private static final String SIGN_DATA   = "in3_signData";

  private IN3 in3;

  public Crypto(IN3 in3) {
    this.in3 = in3;
  }

  /**
     * returns a signature given a message and a key.
     */
  public Signature signData(String msg, String key, SignatureType sigType) {
    return Signature.asSignature(in3.sendRPCasObject(SIGN_DATA, new Object[] {msg, key, JSON.asString(sigType)}));
  }

  public String decryptKey(String key, String passphrase) {
    return JSON.asString(in3.sendRPCasObject(DECRYPT_KEY, new Object[] {key, passphrase}));
  }

  /**
     * extracts the public address from a private key.
     */
  public String pk2address(String key) {
    return JSON.asString(in3.sendRPCasObject(PK2ADDRESS, new Object[] {key}));
  }

  /**
     * extracts the public key from a private key.
     */
  public String pk2public(String key) {
    return JSON.asString(in3.sendRPCasObject(PK2PUBLIC, new Object[] {key}));
  }

  /**
     * extracts the address and public key from a signature.
     */
  public Account ecrecover(String msg, String sig) {
    return ecrecover(msg, sig, null);
  }

  /**
     * extracts the address and public key from a signature.
     */
  public Account ecrecover(String msg, String sig, SignatureType sigType) {
    return Account.asAccount(in3.sendRPCasObject(ECRECOVER, new Object[] {msg, sig, JSON.asString(sigType)}));
  }

  /**
     * returns a signature given a message and a key.
     */
  public Signature signData(String msg, String key) {
    return Signature.asSignature(signData(msg, key, null));
  }
}
