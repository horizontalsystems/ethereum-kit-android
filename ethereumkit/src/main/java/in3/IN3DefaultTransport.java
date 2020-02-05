package in3;

import java.net.*;
import java.io.*;

class IN3DefaultTransport implements IN3Transport {

    @Override
    public byte[][] handle(String[] urls, byte[] payload) {
        byte[][] result = new byte[urls.length][];

        for (int i = 0; i < urls.length; i++) {
            try {
                URL url = new URL(urls[i]);
                URLConnection con = url.openConnection();
                HttpURLConnection http = (HttpURLConnection) con;
                http.setRequestMethod("POST");
                http.setUseCaches(false);
                http.setDoOutput(true);
                http.setRequestProperty("Content-Type", "application/json");
                http.setRequestProperty("Accept", "application/json");
                http.setRequestProperty("charsets", "utf-8");
                http.connect();
                OutputStream os = http.getOutputStream();
                os.write(payload);
                InputStream is = http.getInputStream();

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[1024];
                while ((nRead = is.read(data, 0, data.length)) != -1)
                    buffer.write(data, 0, nRead);

                buffer.flush();
                is.close();
                result[i] = buffer.toByteArray();
            } catch (Exception ex) {
                result[i] = null;
            }
        }
        return result;
    }
}
