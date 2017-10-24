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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.view.View;
import android.view.View.MeasureSpec;


public class RangeVisualisationView extends View {
	
	private int rangeBegin;
	private int rangeEnd;
	private double normalizedRangeBegin;
	private double normalizedRangeEnd;
	private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	public static int BOTTOM_PADDING_PERCENT = 10;
	
	
	/**
     * Default color of a {@link HorizRangeSeekBar}, #FF33B5E5. This is also known as "Ice Cream Sandwich" blue.
     */
    public static final int DEFAULT_COLOR = Color.argb(0xFF, 0x33, 0xB5, 0xE5);

	public RangeVisualisationView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		 paint.setColor(DEFAULT_COLOR);
	     paint.setAntiAlias(true);
	     
	}
	


	
	@Override
	protected synchronized void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		super.onDraw(canvas);
		// remember to draw top line at most in the middle of the screen no to lose perspective
		// find how to draw gradient functions
		
		
	}
	
	public void setRange(int begin, int end){
		
	}




	public int getRangeBegin() {
		return rangeBegin;
	}




	public void setRangeBegin(int rangeBegin) {
		this.rangeBegin = rangeBegin;
	}




	public int getRangeEnd() {
		return rangeEnd;
	}




	public void setRangeEnd(int rangeEnd) {
		this.rangeEnd = rangeEnd;
	}




	public double getNormalizedRangeBegin() {
		return normalizedRangeBegin;
	}




	public void setNormalizedRangeBegin(double normalizedRangeBegin) {
		this.normalizedRangeBegin = normalizedRangeBegin;
		invalidate();
	}




	public double getNormalizedRangeEnd() {
		return normalizedRangeEnd;
	}




	public void setNormalizedRangeEnd(double normalizedRangeEnd) {
		this.normalizedRangeEnd = normalizedRangeEnd;
		invalidate();
	}

}
