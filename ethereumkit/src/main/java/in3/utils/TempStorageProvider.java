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

package in3.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * a simple Storage Provider storing the cache in the temp-folder.
 */
public class TempStorageProvider implements StorageProvider {

  private static File   tmp       = new File(System.getProperty("java.io.tmpdir"));
  private static String in3Prefix = "in3_cache_";

  @Override
  public byte[] getItem(String key) {

    File f = new File(tmp, in3Prefix + key);
    if (f.exists()) {
      BufferedInputStream is = null;
      try {
        is             = new BufferedInputStream(new FileInputStream(f));
        byte[] content = new byte[(int) f.length()];
        int offset     = 0;
        while (offset < content.length)
          offset += is.read(content, offset, content.length - offset);
        return content;
      } catch (Exception ex) {
        return null;
      } finally {
        try {
          if (is != null)
            is.close();
        } catch (IOException io) {
        }
      }
    }
    return null;
  }

  @Override
  public void setItem(String key, byte[] content) {
    try {
      FileOutputStream os = new FileOutputStream(new File(tmp, in3Prefix + key));
      os.write(content);
      os.close();
    } catch (IOException ex) {
    }
  }

  @Override
  public boolean clear() {
    try {
      // TODO perform whatever action necessary to clear cache
      return true;
    } catch (Throwable t) {
      t.printStackTrace();
      return false;
    }
  }
}