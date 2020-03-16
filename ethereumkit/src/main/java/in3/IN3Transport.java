package in3;

/*
 * Interface for the IN3 transport.
 */
interface IN3Transport {
  byte[][] handle(String[] urls, byte[] payload);
}