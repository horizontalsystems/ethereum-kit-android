package in3.config;

/**
 * an Interface class, which is able to generate a JSON-String.
 */
public interface Configuration {
  /**
     * generates a json-string based on the internal data.
     */
  public String toJSON();
}
