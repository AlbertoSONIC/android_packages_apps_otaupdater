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
import java.util.HashMap;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.actionbarsherlock.app.SherlockListFragment;
import com.otaupdater.utils.Config;

public class AboutTab extends SherlockListFragment {

    private final ArrayList<HashMap<String, String>> DATA = new ArrayList<HashMap<String,String>>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PackageInfo pInfo = null;
        try {
            Context ctx = getActivity().getApplicationContext();
            pInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        String version = pInfo == null ? getString(R.string.about_version_unknown) : pInfo.versionName;

        HashMap<String, String> item;

        item = new HashMap<String, String>();
        item.put("title", getString(R.string.about_ota_title));
        item.put("summary", getString(R.string.about_ota_summary));
        DATA.add(item);

        item = new HashMap<String, String>();
        item.put("title", getString(R.string.about_version_title));
        item.put("summary", version);
        DATA.add(item);

        item = new HashMap<String, String>();
        item.put("title", getString(R.string.about_license_title));
        item.put("summary", "");
        DATA.add(item);

        item = new HashMap<String, String>();
        item.put("title", getString(R.string.about_contrib_title));
        item.put("summary", "");
        DATA.add(item);

        item = new HashMap<String, String>();
        item.put("title", getString(R.string.about_feedback_title));
        item.put("summary", "");
        DATA.add(item);

        item = new HashMap<String, String>();
        item.put("title", getString(R.string.about_uptodate));
        item.put("summary", getString(R.string.about_follow_title));
        DATA.add(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list, container, false);
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(new SimpleAdapter(getActivity(),
                DATA,
                R.layout.two_line_icon_list_item,
                new String[] { "title", "summary" },
                new int[] { android.R.id.text1, android.R.id.text2 }));
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        switch (position) {
        case 0:
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Config.WEB_HOME_URL)));
            break;
        case 2:
            startActivity(new Intent(getActivity(), LicenseActivity.class));
            break;
        case 3:
            startActivity(new Intent(getActivity(), ContributorsActivity.class));
            break;
        case 4:
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Config.WEB_FEEDBACK_URL)));
            break;
        case 5:
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Config.GPLUS_URL)));
            break;
        }
    }
}
