/*
 * Copyright (C) 2012 OTA Update Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may only use this file in compliance with the license and provided you are not associated with or are in co-operation anyone by the name 'X Vanderpoel'.
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

package com.ota.updater.two;

import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;
import com.ota.updater.two.utils.Config;
import com.ota.updater.two.utils.KernelInfo;
import com.ota.updater.two.utils.RomInfo;
import com.ota.updater.two.utils.Utils;

public class GCMIntentService extends GCMBaseIntentService {

    public GCMIntentService() {
        super(Config.GCM_SENDER_ID);
    }

    @Override
    protected void onError(Context ctx, String errorID) {
        Log.e(Config.LOG_TAG + "GCMError", errorID);
    }

    @Override
    protected void onMessage(Context ctx, Intent payload) {
        final Config cfg = Config.getInstance(getApplicationContext());

        String msgType = payload.getStringExtra("type");
        if (msgType.equals("rom")) {
            if (!Utils.isRomOtaEnabled()) return;

            RomInfo info = RomInfo.fromIntent(payload);

            if (!Utils.isRomUpdate(info)) {
                Log.v(Config.LOG_TAG + "GCM", "got rom GCM message, not update");
                cfg.clearStoredRomUpdate();
                Utils.clearRomUpdateNotif(getApplicationContext());
                return;
            }

            cfg.storeRomUpdate(info);
            if (cfg.getShowNotif()) {
                Log.v(Config.LOG_TAG + "GCM", "got rom GCM message");
                Utils.showRomUpdateNotif(ctx, info);
            } else {
                Log.v(Config.LOG_TAG + "GCM", "got rom GCM message, notif not shown");
            }
        } else if (msgType.equals("kernel")) {
            if (!Utils.isKernelOtaEnabled()) return;

            KernelInfo info = KernelInfo.fromIntent(payload);

            if (!Utils.isKernelUpdate(info)) {
                Log.v(Config.LOG_TAG + "GCM", "got kernel GCM message, not update");
                cfg.clearStoredKernelUpdate();
                Utils.clearKernelUpdateNotif(getApplicationContext());
                return;
            }

            cfg.storeKernelUpdate(info);
            if (cfg.getShowNotif()) {
                Log.v(Config.LOG_TAG + "GCM", "got kernel GCM message");
                Utils.showKernelUpdateNotif(ctx, info);
            } else {
                Log.v(Config.LOG_TAG + "GCM", "got kernel GCM message, notif not shown");
            }
        }
    }

    @Override
    protected void onRegistered(Context ctx, String regID) {
        Log.v(Config.LOG_TAG + "GCMRegister", "GCM registered - ID=" + regID);
        ArrayList<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        params.add(new BasicNameValuePair("do", "register"));
        params.add(new BasicNameValuePair("reg_id", regID));
        params.add(new BasicNameValuePair("device", android.os.Build.DEVICE.toLowerCase()));
        if (Utils.isRomOtaEnabled()) params.add(new BasicNameValuePair("rom_id", Utils.getRomOtaID()));
        if (Utils.isKernelOtaEnabled()) params.add(new BasicNameValuePair("kernel_id", Utils.getKernelOtaID()));
        params.add(new BasicNameValuePair("device_id", Utils.md5(((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).getDeviceId())));

        try {
            HttpClient http = new DefaultHttpClient();
            HttpPost req = new HttpPost(Config.GCM_REGISTER_URL);
            req.setEntity(new UrlEncodedFormEntity(params));

            HttpResponse r = http.execute(req);
            int status = r.getStatusLine().getStatusCode();
            HttpEntity e = r.getEntity();
            if (status == 200) {
                String data = EntityUtils.toString(e);
                if (data.length() == 0) {
                    Log.w(Config.LOG_TAG + "GCMRegister", "No response to registration");
                    return;
                }
                JSONObject json = new JSONObject(data);

                if (json.length() == 0) {
                    Log.w(Config.LOG_TAG + "GCMRegister", "Empty response to registration");
                    return;
                }

                if (json.has("error")) {
                    Log.e(Config.LOG_TAG + "GCMRegister", json.getString("error"));
                    return;
                }

                final Config cfg = Config.getInstance(getApplicationContext());

                if (Utils.isRomOtaEnabled()) {
                    JSONObject jsonRom = json.getJSONObject("rom");

                    RomInfo info = new RomInfo(
                            jsonRom.getString("rom"),
                            jsonRom.getString("version"),
                            jsonRom.getString("changelog"),
                            jsonRom.getString("url"),
                            jsonRom.getString("md5"),
                            Utils.parseDate(jsonRom.getString("date")));

                    if (Utils.isRomUpdate(info)) {
                        cfg.storeRomUpdate(info);
                        if (cfg.getShowNotif()) {
                        	Utils.showRomUpdateNotif(getApplicationContext(), info);
                        } else {
                            Log.v(Config.LOG_TAG + "GCMRegister", "got rom update response, notif not shown");
                        }
                    } else {
                        cfg.clearStoredRomUpdate();
                        Utils.clearRomUpdateNotif(getApplicationContext());
                    }
                }

                if (Utils.isKernelOtaEnabled()) {
                    JSONObject jsonKernel = json.getJSONObject("rom");

                    KernelInfo info = new KernelInfo(
                            jsonKernel.getString("rom"),
                            jsonKernel.getString("version"),
                            jsonKernel.getString("changelog"),
                            jsonKernel.getString("url"),
                            jsonKernel.getString("md5"),
                            Utils.parseDate(jsonKernel.getString("date")));

                    if (Utils.isKernelUpdate(info)) {
                        cfg.storeKernelUpdate(info);
                        if (cfg.getShowNotif()) {
                            Utils.showKernelUpdateNotif(getApplicationContext(), info);
                        } else {
                            Log.v(Config.LOG_TAG + "GCMRegister", "got kernel update response, notif not shown");
                        }
                    } else {
                        cfg.clearStoredKernelUpdate();
                        Utils.clearKernelUpdateNotif(getApplicationContext());
                    }
                }
            } else {
                if (e != null) e.consumeContent();
                Log.w(Config.LOG_TAG + "GCMRegister", "registration response " + status);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onUnregistered(Context ctx, String regID) {
        Log.v(Config.LOG_TAG + "GCMRegister", "GCM unregistered - ID=" + regID);
        ArrayList<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        params.add(new BasicNameValuePair("do", "unregister"));
        params.add(new BasicNameValuePair("reg_id", regID));

        try {
            HttpClient http = new DefaultHttpClient();
            HttpPost req = new HttpPost(Config.GCM_REGISTER_URL);
            req.setEntity(new UrlEncodedFormEntity(params));
            HttpResponse resp = http.execute(req);
            if (resp.getStatusLine().getStatusCode() != 200) {
                Log.w(Config.LOG_TAG + "GCMRegister", "unregistration response non-200");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
