/*
 * Copyright (C) 2012 OTA Update Center
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

package com.otaupdater;

import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;
import com.otaupdater.utils.Config;
import com.otaupdater.utils.KernelInfo;
import com.otaupdater.utils.PropUtils;
import com.otaupdater.utils.RomInfo;
import com.otaupdater.utils.Utils;

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
        final Context context = getApplicationContext();
        final Config cfg = Config.getInstance(context);

        String msgType = payload.getStringExtra("type");
        if (msgType.equals("rom")) {
            if (!PropUtils.isRomOtaEnabled()) return;

            RomInfo info = RomInfo.fromIntent(payload);

            if (!Utils.isRomUpdate(info)) {
                Log.v(Config.LOG_TAG + "GCM", "got rom GCM message, not update");
                cfg.clearStoredRomUpdate();
                RomInfo.clearUpdateNotif(context);
                return;
            }

            cfg.storeRomUpdate(info);
            if (cfg.getShowNotif()) {
                Log.v(Config.LOG_TAG + "GCM", "got rom GCM message");
                info.showUpdateNotif(context);
            } else {
                Log.v(Config.LOG_TAG + "GCM", "got rom GCM message, notif not shown");
            }
        } else if (msgType.equals("kernel")) {
            if (!PropUtils.isKernelOtaEnabled()) return;

            KernelInfo info = KernelInfo.fromIntent(payload);

            if (!Utils.isKernelUpdate(info)) {
                Log.v(Config.LOG_TAG + "GCM", "got kernel GCM message, not update");
                cfg.clearStoredKernelUpdate();
                KernelInfo.clearUpdateNotif(context);
                return;
            }

            cfg.storeKernelUpdate(info);
            if (cfg.getShowNotif()) {
                Log.v(Config.LOG_TAG + "GCM", "got kernel GCM message");
                info.showUpdateNotif(context);
            } else {
                Log.v(Config.LOG_TAG + "GCM", "got kernel GCM message, notif not shown");
            }
        }
    }

    @Override
    protected void onRegistered(Context ctx, String regID) {
        Log.v(Config.LOG_TAG + "GCMRegister", "GCM registered - ID=" + regID);
        Utils.updateGCMRegistration(ctx, regID);
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
