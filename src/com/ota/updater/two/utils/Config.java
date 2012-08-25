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

package com.ota.updater.two.utils;

import java.io.File;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;

public class Config {
    public static final String LOG_TAG = "OTA::";

    public static final String WEB_HOME_URL = "https://www.otaupdatecenter.pro/";
    public static final String GPLUS_URL = "https://plus.google.com/102074511541445644953/posts";

    public static final String GCM_SENDER_ID = "1068482628480";
    public static final String GCM_REGISTER_URL = "https://www.otaupdatecenter.pro/pages/regdevice2.php";

    public static final String ROM_PULL_URL = "https://www.otaupdatecenter.pro/pages/rominfo.php";
    public static final String KERNEL_PULL_URL = "https://www.otaupdatecenter.pro/pages/kernelinfo.php";

    public static final String OTA_SD_PATH_OS_PROP = "otaupdater.sdcard.os";
    public static final String OTA_SD_PATH_RECOVERY_PROP = "otaupdater.sdcard.recovery";

    public static final int ROM_NOTIF_ID = 1;
    public static final int KERNEL_NOTIF_ID = 2;

    public static final int WAKE_TIMEOUT = 30000;

    public static final String DL_PATH = "/" + Utils.getOSSdPath() + "/OTA-Updater/download/";
    public static final File DL_PATH_FILE = new File(Config.DL_PATH);

    static {
        DL_PATH_FILE.mkdirs();
    }

    private boolean showNotif = true;

    private int lastVersion = -1;
    private String lastDevice = null;
    private String lastRomID = null;
    private String lastKernelID = null;

    private int curVersion = -1;
    private String curDevice = null;
    private String curRomID = null;
    private String curKernelID = null;

    private RomInfo storedRomUpdate = null;
    private KernelInfo storedKernelUpdate = null;

    private static final String PREFS_NAME = "prefs";
    private final SharedPreferences PREFS;

    private Config(Context ctx) {
        PREFS = ctx.getApplicationContext().getSharedPreferences(PREFS_NAME, 0);

        showNotif = PREFS.getBoolean("showNotif", showNotif);

        lastDevice = PREFS.getString("device", lastDevice);
        lastVersion = PREFS.getInt("version", lastVersion);
        lastRomID = PREFS.getString("rom_id", lastRomID);
        lastKernelID = PREFS.getString("kernel_id", lastKernelID);

        if (PREFS.contains("rom_info_name")) {
            storedRomUpdate = new RomInfo(PREFS.getString("rom_info_name", null),
                    PREFS.getString("rom_info_version", null),
                    PREFS.getString("rom_info_changelog", null),
                    PREFS.getString("rom_info_url", null),
                    PREFS.getString("rom_info_md5", null),
                    Utils.parseDate(PREFS.getString("rom_info_date", null)));
        }

        if (PREFS.contains("kernel_info_name")) {
            storedKernelUpdate = new KernelInfo(PREFS.getString("kernel_info_name", null),
                    PREFS.getString("kernel_info_version", null),
                    PREFS.getString("kernel_info_changelog", null),
                    PREFS.getString("kernel_info_url", null),
                    PREFS.getString("kernel_info_md5", null),
                    Utils.parseDate(PREFS.getString("kernel_info_date", null)));
        }

        try {
            curVersion = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionCode;
        } catch (NameNotFoundException e) {
        }
        curDevice = android.os.Build.DEVICE.toLowerCase();
        curRomID = Utils.isRomOtaEnabled() ? Utils.getRomOtaID() : null;
        curKernelID = Utils.isKernelOtaEnabled() ? Utils.getKernelOtaID() : null;
    }
    private static Config instance = null;
    public static synchronized Config getInstance(Context ctx) {
        if (instance == null) instance = new Config(ctx);
        return instance;
    }

    public boolean getShowNotif() {
        return showNotif;
    }

    public void setShowNotif(boolean showNotif) {
        this.showNotif = showNotif;
        synchronized (PREFS) {
            SharedPreferences.Editor editor = PREFS.edit();
            editor.putBoolean("showNotif", showNotif);
            editor.commit();
        }
    }

    public int getLastVersion() {
        return lastVersion;
    }

    public String getLastDevice() {
        return lastDevice;
    }

    public String getLastRomID() {
        return lastRomID;
    }

    public void setValuesToCurrent() {
        synchronized (PREFS) {
            SharedPreferences.Editor editor = PREFS.edit();
            editor.putInt("version", curVersion);
            editor.putString("device", curDevice);
            editor.putString("romid", curRomID);
            editor.commit();
        }
    }

    public boolean upToDate() {
        if (lastDevice == null) return false;

        boolean romIdUpToDate = true;
        if (Utils.isRomOtaEnabled()) {
            if (lastRomID == null || curRomID == null) romIdUpToDate = false;
            else romIdUpToDate = curRomID.equals(lastRomID);
        } else if (lastRomID != null) romIdUpToDate = false;

        boolean kernelIdUpToDate = true;
        if (Utils.isKernelOtaEnabled()) {
            if (lastKernelID == null || curKernelID == null) kernelIdUpToDate = false;
            else kernelIdUpToDate = curKernelID.equals(lastKernelID);
        } else if (lastKernelID != null) kernelIdUpToDate = false;

        return curVersion == lastVersion && curDevice.equals(lastDevice) && romIdUpToDate && kernelIdUpToDate;
    }

    public boolean hasStoredRomUpdate() {
        return storedRomUpdate != null;
    }

    public RomInfo getStoredRomUpdate() {
        return storedRomUpdate;
    }

    public void storeRomUpdate(RomInfo info) {
        synchronized (PREFS) {
            SharedPreferences.Editor editor = PREFS.edit();
            editor.putString("rom_info_name", info.romName);
            editor.putString("rom_info_version", info.version);
            editor.putString("rom_info_changelog", info.changelog);
            editor.putString("rom_info_url", info.url);
            editor.putString("rom_info_md5", info.md5);
            editor.putString("rom_info_date", Utils.formatDate(info.date));
            editor.commit();
        }
    }

    public void clearStoredRomUpdate() {
        synchronized (PREFS) {
            SharedPreferences.Editor editor = PREFS.edit();
            editor.remove("rom_info_name");
            editor.remove("rom_info_version");
            editor.remove("rom_info_changelog");
            editor.remove("rom_info_url");
            editor.remove("rom_info_md5");
            editor.remove("rom_info_date");
            editor.commit();
        }
    }

    public boolean hasStoredKernelUpdate() {
        return storedKernelUpdate != null;
    }

    public KernelInfo getStoredKernelUpdate() {
        return storedKernelUpdate;
    }

    public void storeKernelUpdate(KernelInfo info) {
        synchronized (PREFS) {
            SharedPreferences.Editor editor = PREFS.edit();
            editor.putString("kernel_info_name", info.kernelName);
            editor.putString("kernel_info_version", info.version);
            editor.putString("kernel_info_changelog", info.changelog);
            editor.putString("kernel_info_url", info.url);
            editor.putString("kernel_info_md5", info.md5);
            editor.putString("kernel_info_date", Utils.formatDate(info.date));
            editor.commit();
        }
    }

    public void clearStoredKernelUpdate() {
        synchronized (PREFS) {
            SharedPreferences.Editor editor = PREFS.edit();
            editor.remove("kernel_info_name");
            editor.remove("kernel_info_version");
            editor.remove("kernel_info_changelog");
            editor.remove("kernel_info_url");
            editor.remove("kernel_info_md5");
            editor.remove("kernel_info_date");
            editor.commit();
        }
    }
}
