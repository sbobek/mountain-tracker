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

package pl.wownow.view;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class ArDisplayView extends SurfaceView implements
		SurfaceHolder.Callback {
	Camera mCamera;
	SurfaceHolder mHolder;
	Activity mActivity;

	public ArDisplayView(Context context, Activity activity) {
		super(context);
		mActivity = activity;
		mHolder = getHolder();

		// This value is supposedly deprecated and set "automatically" when
		// needed.
		// Without this, the application crashes.
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		// callbacks implemented by ArDisplayView
		mHolder.addCallback(this);
	}

	public void surfaceCreated(SurfaceHolder holder) {

		// Grab the camera
		mCamera = Camera.open();

		// Set Display orientation
		CameraInfo info = new CameraInfo();
		Camera.getCameraInfo(CameraInfo.CAMERA_FACING_BACK, info);

		int rotation = mActivity.getWindowManager().getDefaultDisplay()
				.getRotation(); 
		int degrees = 0;
		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}
		mCamera.setDisplayOrientation((info.orientation - degrees + 360) % 360);

		try {
			mCamera.setPreviewDisplay(mHolder);
		} catch (IOException e) {

		}
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Camera.Parameters params = mCamera.getParameters();

		// Find an appropriate preview size that fits the surface
		List<Size> prevSizes = params.getSupportedPreviewSizes();
		for (Size s : prevSizes) {
			if ((s.height <= height) && (s.width <= width)) {
				params.setPreviewSize(s.width, s.height);
				break;
			}

		}

		// Set the preview format
		//params.setPreviewFormat(ImageFormat.JPEG);

		// Consider adjusting frame rate to appropriate rate for AR

		// Confirm the parameters
		mCamera.setParameters(params);
		
		

		// Begin previewing
		mCamera.startPreview();
	}

	public void surfaceDestroyed(SurfaceHolder holder) {

		// Shut down camera preview
		mCamera.stopPreview();
		mCamera.release();
	}

}
