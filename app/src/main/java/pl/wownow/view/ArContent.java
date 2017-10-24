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

import java.util.LinkedList;


import android.content.Context;
import android.view.View;

public  class ArContent extends View {
	
	int viewWidth;
	int viewHeight;

	private LinkedList<AugmentedData> data;

	public ArContent(Context context) {
		super(context);
		data = new LinkedList<AugmentedData>();
	}
	
	public synchronized void setDataToDraw(LinkedList<AugmentedData> data){
		this.data = data;
		this.invalidate();
	}
	
	public synchronized LinkedList<AugmentedData> getDataToDraw(){
		return data;
	}
	
	public int getViewHeight() {
		return viewHeight;
	}
	public int getViewWidth() {
		return viewWidth;
	}
	
	@Override
	 protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld){
	     super.onSizeChanged(xNew, yNew, xOld, yOld);

	     viewWidth = xNew;
	     viewHeight = yNew;
	}

}
