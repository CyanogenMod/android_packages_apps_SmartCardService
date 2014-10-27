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
import org.simalliance.openmobileapi.service.security.arf.PKCS15.EFCDF;
import org.simalliance.openmobileapi.service.security.arf.SecureElement;
import org.simalliance.openmobileapi.service.security.arf.SecureElementException;
import org.simalliance.openmobileapi.service.Util;
import org.simalliance.openmobileapi.service.security.arf.PKCS15.PKCS15Exception;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.Certificate;
import android.util.Log;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.ByteArrayInputStream;

public class EFTCF extends EF {

    public static final String TAG = "EFTCF";
    // DT ID for EF_TCF file
    public static final byte[] EFTCF_PATH = { 0x20,0x03 };
    public static ArrayList<X509Certificate> x509certificates = new ArrayList<X509Certificate>();
    public static String byteArrayToHex(byte[] a) {
       StringBuilder sb = new StringBuilder(a.length * 2);
       for(byte b: a)
          sb.append(String.format("%02x", b & 0xff));
       return sb.toString();
    }

    private int bytesToInt(byte a, byte b) {
        String ab = String.format("%02X", a) + String.format("%02X", b);
        return Integer.parseInt(ab, 16);
    }
    /**
     * Constructor
     * @param secureElement SE on which ISO7816 commands are applied
     */
    public EFTCF(SecureElement handle) throws PKCS15Exception,SecureElementException, CertificateException {
        super(handle);

    }

    public ArrayList<X509Certificate> analyseFile()  throws PKCS15Exception,SecureElementException, CertificateException {
        ArrayList<byte[]> couples = EFCDF.returnCouples();
        byte[] buff = null;
        byte[] path = EFTCF_PATH;
        byte[] TCF_File = null;
        Certificate UiccCert;
        Log.v(TAG,"Analysing EF_TCF...");

        if ( selectFile(path)!= APDU_SUCCESS) {
            Log.v(TAG, "EF_TCF not found!!");
            return null;
        }
        TCF_File = readBinary(0,Util.END);
        Log.v(TAG, "EF_TCF DER ....size = " + TCF_File.length);

        Log.v(TAG, "Number of Certificate x509 found in the CDF : " + couples.size());
        for (int i = 0; i < couples.size(); i++) {

            int Startbuff = bytesToInt(couples.get(i)[0],couples.get(i)[1]);
            int Sizebuff = bytesToInt(couples.get(i)[2],couples.get(i)[3]);
            Log.v(TAG, "Certificate number " + (i+1) + " EFTCF Certificate x509 buff start is byte " + Startbuff);
            Log.v(TAG, "Certificate number " + (i+1) + " EFTCF Certificate x509 buff Size is " + Sizebuff);
            UiccCert = decodeCertificate (Arrays.copyOfRange(TCF_File, Startbuff,Startbuff + Sizebuff));
        }
        if (x509certificates.size()>0) {
            return x509certificates;
        } else {
            return null;
        }
    }

    public static Certificate decodeCertificate(byte[] certData) throws CertificateException {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) certFactory
                .generateCertificate(new ByteArrayInputStream(certData));
        x509certificates.add(cert);
        return cert;
    }
}
