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

package in3;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.security.MessageDigest;
import java.util.Arrays;

public class Loader {

  private static boolean loaded = false;
  /*
        Based on the assumptions of: https://developer.android.com/ndk/guides/abis.html
        The name has to be the same of the target library of CMakeLists.txt
    */
  private static final String TARGET_LINK_LIBRARY = "in3_jni";

  private static String getLibName() {
    final String os    = System.getProperty("os.name").toLowerCase();
    final String arch  = System.getProperty("os.arch").toLowerCase();
    final String model = System.getProperty("sun.arch.data.model");
    if (os.indexOf("linux") >= 0 && arch.indexOf("arm") >= 0) {
      return "in3_jni_arm";
    }
    if ("32".equals(model)) return "in3_jni_32";

    return TARGET_LINK_LIBRARY;
  }

  private static byte[] md5(InputStream is) throws IOException {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.reset();
      byte[] bytes = new byte[2048];
      int numBytes;
      while ((numBytes = is.read(bytes)) != -1)
        md.update(bytes, 0, numBytes);
      return md.digest();

    } catch (Exception ex) {
      throw new IOException(ex);
    } finally {
      is.close();
    }
  }

  public static void loadLibrary() {
    if (loaded)
      return;
    loaded = true;

    try {
      // try to load it from the path
      System.loadLibrary(TARGET_LINK_LIBRARY);
      return;
    } catch (java.lang.UnsatisfiedLinkError ignored) {}

    // ok, not found, so we use the one in the package.

    String libFileName = System.mapLibraryName(getLibName());
    String jarPath     = "/in3/native/" + libFileName;

    URL src = Loader.class.getResource(jarPath);
    if (src == null)
      throw new RuntimeException("Could not load the library for " + jarPath);

    try {
      File lib = new File(new File(System.getProperty("java.io.tmpdir")), libFileName);
      if (lib.exists() && !Arrays.equals(md5(src.openStream()), md5(new FileInputStream(lib))) && !lib.delete())
        throw new IOException(
            "Could not delete the library from temp-file! Maybe some other proccess is still using it ");

      if (!lib.exists()) {
        InputStream  is = null;
        OutputStream os = null;
        try {
          is            = src.openStream();
          os            = new FileOutputStream(lib);
          byte[] buffer = new byte[4096];
          int read      = 0;
          while ((read = is.read(buffer)) >= 0)
            os.write(buffer, 0, read);
        } finally {
          if (is != null)
            is.close();
          if (os != null)
            os.close();
        }
        if (!System.getProperty("os.name").contains("Windows")) {
          try {
            Runtime.getRuntime().exec(new String[] {"chmod", "755", lib.getAbsolutePath()}).waitFor();
          } catch (Throwable e) {
          }
        }
      }
      System.load(lib.getAbsolutePath());

    } catch (Exception ex) {
      throw new RuntimeException("Could not load the native library ", ex);
    }
  }
}
