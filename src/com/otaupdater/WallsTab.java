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
import java.util.Arrays;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.Spinner;

import com.actionbarsherlock.app.SherlockFragment;
import com.otaupdater.utils.ImageAdapter;

public class WallsTab extends SherlockFragment implements OnItemSelectedListener {
	private int screenHeight;
	private int screenWidth;
	private String deviceRes;

    public int getShownIndex() {
        return getArguments().getInt("index", 0);
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		DisplayMetrics metrics = new DisplayMetrics();
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        display.getMetrics(metrics);

        int orient = display.getRotation();
        if (orient == Surface.ROTATION_90 || orient == Surface.ROTATION_270) {
            screenWidth = metrics.heightPixels;
            screenHeight = metrics.widthPixels;
        } else {
            screenWidth = metrics.widthPixels;
            screenHeight = metrics.heightPixels;
        }

        deviceRes = Integer.toString(screenWidth) + "x" + Integer.toString(screenHeight);
	}

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.walls, container, false);

		ArrayList<String> resValues = new ArrayList<String>(Arrays.asList(getResources().getStringArray(R.array.res_array)));
		if (!resValues.contains(deviceRes)) resValues.add(deviceRes);

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_expandable_list_item_1, resValues);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        int resIdx = adapter.getPosition(deviceRes);

		Spinner spinner = (Spinner) v.findViewById(R.id.spinner);
        spinner.setAdapter(adapter);
		spinner.setSelection(resIdx);
		spinner.setOnItemSelectedListener(this);

	    GridView gridview = (GridView) v.findViewById(R.id.gridview);
	    gridview.setAdapter(new ImageAdapter(getActivity()));
        return v;
    }

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
		// TODO fetch wallpapers from server
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) { }
}
