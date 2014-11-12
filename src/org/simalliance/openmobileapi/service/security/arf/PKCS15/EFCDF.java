/*
    Copyright (c) 2014, The Linux Foundation. All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are
    met:
        * Redistributions of source code must retain the above copyright
          notice, this list of conditions and the following disclaimer.
        * Redistributions in binary form must reproduce the above
          copyright notice, this list of conditions and the following
          disclaimer in the documentation and/or other materials provided
          with the distribution.
        * Neither the name of The Linux Foundation nor the names of its
          contributors may be used to endorse or promote products derived
          from this software without specific prior written permission.

   THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
   WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
   ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
   BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
   CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
   SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
   BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
   WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
   OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
   IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.simalliance.openmobileapi.service.security.arf.PKCS15;

import org.simalliance.openmobileapi.service.security.arf.DERParser;
import org.simalliance.openmobileapi.service.security.arf.SecureElement;
import org.simalliance.openmobileapi.service.security.arf.SecureElementException;
import org.simalliance.openmobileapi.service.Util;
import org.simalliance.openmobileapi.service.security.arf.PKCS15.PKCS15Exception;

import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * EF_CDF related features
 ***************************************************/
public class EFCDF extends EF {

    public static final String TAG = "SmartcardService ACE ARF";
    // Standardized ID for EF_CDF file
    public static final byte[] EFCDF_PATH = { 0x50,0x03 };
    public static ArrayList<byte[]> x509Bytes = null;
    private short DerIndex, DerSize  = 0;

    public byte[] isx509(byte[] buffer)
    throws PKCS15Exception {

        DerSize = (short)buffer.length;
        x509Bytes = new ArrayList<byte[]>();
        byte[] start =null;
        byte[] size = new byte[4];

        DERParser DER=new DERParser(buffer);
        if (DerIndex==DerSize) return null;
        while(++DerIndex<DerSize) {
            if ( (buffer[DerIndex] == (byte)0x20) && (buffer[DerIndex+1] == (byte)0x03) ) {
                if ( buffer[DerIndex+3] == 0x01) {
                    start = new byte[]{ buffer[DerIndex+4] };
                    size =  new byte[]{ buffer[DerIndex+7],  buffer[DerIndex+8] };
                } else if ( buffer[DerIndex+3] == 0x02) {
                    start = new byte[]{ buffer[DerIndex+4], buffer[DerIndex+5] };
                    size =  new byte[]{ buffer[DerIndex+8],  buffer[DerIndex+9] };
                }

                byte[] Certbuff = new byte[4];
                Log.v(TAG, "Found x509 !!!!  start.length " + start.length);
                if (start.length == 1) {
                    System.arraycopy(start,0,Certbuff,1,start.length);
                    System.arraycopy(size,0,Certbuff,start.length + 1,size.length);
                } else {
                    System.arraycopy(start,0,Certbuff,0,start.length);
                    System.arraycopy(size,0,Certbuff,start.length,size.length);
                }
                Log.v(TAG, "Found x509 !!!!  start after the byte " + byteArrayToHex(start) + " size : " + byteArrayToHex(size));
                x509Bytes.add(Certbuff);
            }
        }
        return null;
    }

    public static ArrayList<byte[]> returnCouples (){
        return x509Bytes;
    }

    public static String byteArrayToHex(byte[] a) {
       StringBuilder sb = new StringBuilder(a.length * 2);
       for(byte b: a)
          sb.append(String.format("%02x", b & 0xff));
       return sb.toString();
    }

    /**
     * Constructor
     * @param secureElement SE on which ISO7816 commands are applied
     */
    public EFCDF(SecureElement handle) {
        super(handle);
    }

    /**
     * Selects and Analyses EF_ODF file
     * @return Path to "EF_DODF" from "DODF Tag" entry;
     *             <code>null</code> otherwise
     */
    public boolean checkCDF()  throws PKCS15Exception,SecureElementException {
        Log.v(TAG,"Analysing EF_CDF...");
        byte[] path = EFCDF_PATH;
        if ( selectFile(path)!= APDU_SUCCESS) {
            Log.v(TAG,"EF_CDF not found!!");
            return false;
        } else {
            isx509(readBinary(0,Util.END));
            return true;
        }
    }

}
