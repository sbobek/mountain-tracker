/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright (c) 2017 by RIOT (http://riot.agency)
 *
 */

package pl.wownow.main;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * Dialog to prompt users to enable GPS on the device.
 */
public class EnableGpsDialogFragment extends DialogFragment {
	
	DialogInterface.OnClickListener listener;
	
	public EnableGpsDialogFragment() {
		// TODO Auto-generated constructor stub
	}
	
	public void setListener(DialogInterface.OnClickListener listener){
		this.listener = listener;
	}

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.enable_gps_caption)
                .setMessage(R.string.enable_gps_message)
                .setPositiveButton(R.string.enable_gps_caption,listener)
                .create();
    }
}