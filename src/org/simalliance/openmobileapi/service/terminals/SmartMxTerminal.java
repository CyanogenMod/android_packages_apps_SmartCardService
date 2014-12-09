/*
 * Copyright (C) 2011, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Contributed by: Giesecke & Devrient GmbH.
 */

package org.simalliance.openmobileapi.service.terminals;

import android.content.Context;
import android.nfc.INfcAdapterExtras;
import android.nfc.NfcAdapter;
import com.android.qcom.nfc_extras.*;
import android.os.Binder;
import android.os.Bundle;
import android.util.Log;
import android.os.AsyncTask;


import java.util.MissingResourceException;
import java.util.NoSuchElementException;

import org.simalliance.openmobileapi.service.CardException;
import org.simalliance.openmobileapi.service.SmartcardService;
import org.simalliance.openmobileapi.service.Terminal;


public class SmartMxTerminal extends Terminal {

    private final String TAG;

    private Binder binder = new Binder();

    private final int mSeId;

    private NfcQcomAdapter mNfcQcomAdapter;
    GetNfcQcomAdapterTask mGetNfcQcomAdapterTask;

    private class GetNfcQcomAdapterTask extends AsyncTask<Void, Void, Void > {

        Context context;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        public GetNfcQcomAdapterTask(Context context){
            this.context = context;
        }
        @Override
        protected Void doInBackground(Void... unused) {
            for(int tries = 0; tries < 3; tries++) {
                try {
                    mNfcQcomAdapter = NfcQcomAdapter.getNfcQcomAdapter(context);
                    if (mNfcQcomAdapter == null) {
                        Log.d (TAG, "mNfcQcomAdapter is NULL");
                    } else {
                        Log.d (TAG, "acquired NfcQcomAdapter");
                        return null;
                    }
                } catch (UnsupportedOperationException e) {
                    String errorMsg = "SmartMxTerminal() gracefully failing to acquire NfcQcomAdapter at boot. try" + tries;
                    Log.e(TAG, errorMsg);
                    new Throwable(TAG + ": " + errorMsg, e);
                    e.printStackTrace();
                }
                try {
                    wait(5000);
                } catch (Exception e) {
                    Log.d(TAG, "Interupted while waiting for NfcQcomAdapter by " + e);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            mGetNfcQcomAdapterTask = null;
        }
    }

    public SmartMxTerminal(Context context, int seId) {
        super(SmartcardService._eSE_TERMINAL + SmartcardService._eSE_TERMINAL_EXT[seId], context);
        mSeId = seId;
        TAG = SmartcardService._eSE_TERMINAL + SmartcardService._eSE_TERMINAL_EXT[seId];
        mGetNfcQcomAdapterTask = new GetNfcQcomAdapterTask(context);
        mGetNfcQcomAdapterTask.execute();
    }

    public boolean isCardPresent() throws CardException {
        try {
            if (mNfcQcomAdapter != null) {
                return mNfcQcomAdapter.isSeEnabled(SmartcardService._eSE_TERMINAL + SmartcardService._eSE_TERMINAL_EXT[mSeId]);
            } else {
                Log.d (TAG, "cannot get NfcQcomAdapter");
                return false;
            }
        } catch (Exception e) {
            Log.e (TAG, e.getMessage());
            return false;
        }
    }

    @Override
    protected void internalConnect() throws CardException {

        try {
            if (mNfcQcomAdapter != null) {
                mNfcQcomAdapter.selectSEToOpenApduGate(SmartcardService._eSE_TERMINAL + SmartcardService._eSE_TERMINAL_EXT[mSeId]);
            } else {
                throw new CardException("cannot get NfcQcomAdapter");
            }

            Bundle b = mNfcQcomAdapter.open(binder);
            if (b == null) {
                throw new CardException("open SE failed");
            }
        } catch (Exception e) {
            throw new CardException("open SE failed:" + e.getMessage());
        }
        mDefaultApplicationSelectedOnBasicChannel = true;
        mIsConnected = true;
    }

    @Override
    protected void internalDisconnect() throws CardException {
        try {
            if (mNfcQcomAdapter != null) {
                Bundle b = mNfcQcomAdapter.close(binder);
                if (b == null) {
                    throw new CardException("close SE failed");
                }
            } else {
                throw new CardException("cannot get NfcQcomAdapter");
            }
        } catch (Exception e) {
            throw new CardException("close SE failed");
        }
    }

    @Override
    protected byte[] internalTransmit(byte[] command) throws CardException {
        Bundle b;
        try {
            if (mNfcQcomAdapter != null) {
                b = mNfcQcomAdapter.transceive(command);
                if (b == null) {
                    throw new CardException("exchange APDU failed");
                }
            } else {
                throw new CardException("cannot get NfcQcomAdapter");
            }
            return b.getByteArray("out");
        } catch (Exception e) {
            throw new CardException("exchange APDU failed");
        }
    }

    @Override
    protected int internalOpenLogicalChannel() throws Exception {

        mSelectResponse = null;
        byte[] manageChannelCommand = new byte[] {
                0x00, 0x70, 0x00, 0x00, 0x01
        };
        byte[] rsp = transmit(manageChannelCommand, 2, 0x9000, 0, "MANAGE CHANNEL");
        if ((rsp.length == 2) && ((rsp[0] == (byte) 0x68) && (rsp[1] == (byte) 0x81))) {
            throw new NoSuchElementException("logical channels not supported");
        }
        if (rsp.length == 2 && (rsp[0] == (byte) 0x6A && rsp[1] == (byte) 0x81)) {
            throw new MissingResourceException("no free channel available", "", "");
        }
        if (rsp.length != 3) {
            throw new MissingResourceException("unsupported MANAGE CHANNEL response data", "", "");
        }
        int channelNumber = rsp[0] & 0xFF;
        if (channelNumber == 0 || channelNumber > 19) {
            throw new MissingResourceException("invalid logical channel number returned", "", "");
        }

        return channelNumber;
    }

    @Override
    protected int internalOpenLogicalChannel(byte[] aid) throws Exception {

        if (aid == null) {
            throw new NullPointerException("aid must not be null");
        }
        mSelectResponse = null;

        byte[] manageChannelCommand = new byte[] {
                0x00, 0x70, 0x00, 0x00, 0x01
        };
        byte[] rsp = transmit(manageChannelCommand, 2, 0x9000, 0, "MANAGE CHANNEL");
        if ((rsp.length == 2) && ((rsp[0] == (byte) 0x68) && (rsp[1] == (byte) 0x81))) {
            throw new NoSuchElementException("logical channels not supported");
        }
        if (rsp.length == 2 && (rsp[0] == (byte) 0x6A && rsp[1] == (byte) 0x81)) {
            throw new MissingResourceException("no free channel available", "", "");
        }
        if (rsp.length != 3) {
            throw new MissingResourceException("unsupported MANAGE CHANNEL response data", "", "");
        }
        int channelNumber = rsp[0] & 0xFF;
        if (channelNumber == 0 || channelNumber > 19) {
            throw new MissingResourceException("invalid logical channel number returned", "", "");
        }

        byte[] selectCommand = new byte[aid.length + 6];
        selectCommand[0] = (byte) channelNumber;
        if (channelNumber > 3) {
            selectCommand[0] |= 0x40;
        }
        selectCommand[1] = (byte) 0xA4;
        selectCommand[2] = 0x04;
        selectCommand[4] = (byte) aid.length;
        System.arraycopy(aid, 0, selectCommand, 5, aid.length);
        try {
            mSelectResponse = transmit(selectCommand, 2, 0x9000, 0, "SELECT");
            int sw1 = mSelectResponse[mSelectResponse.length - 2] & 0xFF;
            int sw2 = mSelectResponse[mSelectResponse.length - 1] & 0xFF;
            int sw = (sw1 << 8) | sw2;
            // banking application returns 0x6283 to indicate this AID is locked
            if ((sw != 0x9000)&&(sw != 0x6283)) {
                internalCloseLogicalChannel(channelNumber);
                throw new NoSuchElementException("SELECT CMD failed - SW is not 0x9000/0x6283");
            }
        } catch (CardException exp) {
            internalCloseLogicalChannel(channelNumber);
            throw new NoSuchElementException(exp.getMessage());
        }

        return channelNumber;
    }

    @Override
    protected void internalCloseLogicalChannel(int channelNumber) throws CardException {
        if (channelNumber > 0) {
            byte cla = (byte) channelNumber;
            if (channelNumber > 3) {
                cla |= 0x40;
            }
            byte[] manageChannelClose = new byte[] {
                    cla, 0x70, (byte) 0x80, (byte) channelNumber
            };
            transmit(manageChannelClose, 2, 0x9000, 0xFFFF, "MANAGE CHANNEL");
        }
    }
}
