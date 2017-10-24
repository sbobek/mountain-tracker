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

package pl.wownow.datasource;

import android.location.Location;


public class Peak {
	private String name;
	private Location location;

	
	public Peak(String name, double lat, double lon, int ele){
		this.name = name;
		location = new Location("manual");
		location.setLatitude(lat);
		location.setLongitude(lon);
		location.setAltitude(ele);
	}

	public double getDistanceTo(Location dest) {
		return location.distanceTo(dest);
	}
	
	public Location getLocation() {
		return location;
	}



	public double getEle() {
		return location.getAltitude();
	}

	public void setEle(double ele) {
		location.setAltitude(ele);
	}

	public double getLon() {
		return location.getLongitude();
	}

	public void setLon(double lon) {
		location.setLongitude(lon);
	}

	public double getLat() {
		return location.getLatitude();
	}

	public void setLat(double lat) {
		location.setLatitude(lat);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
