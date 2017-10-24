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


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;

import pl.wownow.datasource.Peak;
import pl.wownow.view.ArContent;
import pl.wownow.view.AugmentedData;
import pl.wownow.view.ScreenCoordinates;

public class ArLocationContent extends ArContent {
	
	private Paint targetPaint;
	
	//DEBUG
	String[] debugInfo = null;

	public ArLocationContent(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		
		targetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		targetPaint.setColor(Color.argb(0xFF, 0x33, 0xB5, 0xE5));
		
	}
	
	/**
	 * FUNCTION FOR DEBUGGING, REMOVE FOR REALEASE
	 * 
	 * @param debugInfo
	 */
	public void setDebugInformation(String[] debugInfo){
		this.debugInfo = debugInfo;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		super.onDraw(canvas);
		for(AugmentedData d: getDataToDraw()){
			ScreenCoordinates sc = d.getScreenCoordinates();
			if(sc != null){		
				//canvas.save();
				float x = sc.getX();
				float y = sc.getY();
				if( x > getViewWidth() || y > getViewHeight()) continue;
				canvas.drawCircle(x, y, 8.0f, targetPaint);	
				Peak p = (Peak)d.getData();
				String name = p.getName();
				String ele = Double.toString(p.getEle());
				canvas.drawText(name, x-50, y-20, targetPaint);
				canvas.drawText(ele, x-50, y-10, targetPaint);
				//canvas.restore();
			}
		}
		if(debugInfo != null){
			int y  = 20;
			for(String i:debugInfo){
				canvas.drawText(i, 0, y, targetPaint);
				y+=20;
			}
		}

	}


}
