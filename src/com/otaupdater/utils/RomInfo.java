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

package com.otaupdater.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.otaupdater.R;

public class RomInfo {
    public String romName;
    public String version;
    public String changelog;
    public String url;
    public String md5;
    public Date date;

    public RomInfo(String romName, String version, String changelog, String downurl, String md5, Date date) {
        this.romName = romName;
        this.version = version;
        this.changelog = changelog;
        this.url = downurl;
        this.md5 = md5;
        this.date = date;
    }

    public static RomInfo fromIntent(Intent i) {
        return new RomInfo(
                i.getStringExtra("rom_info_name"),
                i.getStringExtra("rom_info_version"),
                i.getStringExtra("rom_info_changelog"),
                i.getStringExtra("rom_info_url"),
                i.getStringExtra("rom_info_md5"),
                Utils.parseDate(i.getStringExtra("rom_info_date")));
    }

    public void addToIntent(Intent i) {
        i.putExtra("rom_info_name", romName);
        i.putExtra("rom_info_version", version);
        i.putExtra("rom_info_changelog", changelog);
        i.putExtra("rom_info_url", url);
        i.putExtra("rom_info_md5", md5);
        i.putExtra("rom_info_date", Utils.formatDate(date));
    }

    @TargetApi(11)
    public long fetchFile(Context ctx) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle(ctx.getString(R.string.notif_download));
        request.setDescription(romName);
        request.setVisibleInDownloadsUi(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        }

        request.setDestinationUri(Uri.fromFile(
                new File(Config.ROM_DL_PATH_FILE, Utils.sanitizeName(romName + "__" + version + ".zip"))));

        int netTypes = DownloadManager.Request.NETWORK_WIFI;
        if (!Config.getInstance(ctx).getWifiOnlyDl()) netTypes |= DownloadManager.Request.NETWORK_MOBILE;
        request.setAllowedNetworkTypes(netTypes);

        DownloadManager manager = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
        return manager.enqueue(request);
    }

    public void showUpdateDialog(Context ctx) {
        //TODO show dialog (+kernel if necessary)
    }

    public static void fetchInfo(Context ctx) {
        fetchInfo(ctx, null);
    }

    public static void fetchInfo(Context ctx, RomInfoListener callback) {
        new FetchInfoTask(ctx, callback).execute();
    }

    protected static class FetchInfoTask extends AsyncTask<Void, Void, RomInfo> {
        private RomInfoListener callback = null;
        private Context context = null;
        private String error = null;

        public FetchInfoTask(Context ctx, RomInfoListener callback) {
            this.context = ctx;
            this.callback = callback;
        }

        @Override
        public void onPreExecute() {
            if (callback != null) callback.onStartLoading();
        }

        @Override
        protected RomInfo doInBackground(Void... notused) {
            if (!Utils.isRomOtaEnabled()) {
                error = context.getString(R.string.rom_unsupported_title);
                return null;
            }
            if (!Utils.dataAvailable(context)) {
                error = context.getString(R.string.alert_nodata_title);
                return null;
            }

            try {
                ArrayList<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
                params.add(new BasicNameValuePair("device", android.os.Build.DEVICE.toLowerCase()));
                params.add(new BasicNameValuePair("rom", Utils.getRomOtaID()));

                HttpClient client = new DefaultHttpClient();
                HttpGet get = new HttpGet(Config.ROM_PULL_URL + "?" + URLEncodedUtils.format(params, "UTF-8"));
                HttpResponse r = client.execute(get);
                int status = r.getStatusLine().getStatusCode();
                HttpEntity e = r.getEntity();
                if (status == 200) {
                    String data = EntityUtils.toString(e);
                    JSONObject json = new JSONObject(data);

                    if (json.has("error")) {
                        Log.e(Config.LOG_TAG + "Fetch", json.getString("error"));
                        error = json.getString("error");
                        return null;
                    }

                    return new RomInfo(
                            json.getString("rom"),
                            json.getString("version"),
                            json.getString("changelog"),
                            json.getString("url"),
                            json.getString("md5"),
                            Utils.parseDate(json.getString("date")));
                } else {
                    if (e != null) e.consumeContent();
                    error = "Server responded with error " + status;
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                error = e.getMessage();
            }

            return null;
        }

        @Override
        public void onPostExecute(RomInfo result) {
            if (callback != null) {
                if (result != null) callback.onLoaded(result);
                else callback.onError(error);
            }
        }
    }

    public static interface RomInfoListener {
        void onStartLoading();
        void onLoaded(RomInfo info);
        void onError(String err);
    }
}
