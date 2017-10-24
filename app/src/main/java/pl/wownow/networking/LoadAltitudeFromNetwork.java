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

package pl.wownow.networking;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.xmlpull.v1.XmlPullParserException;

import pl.wownow.augmented.AugmentedActivity;
import pl.wownow.xml.MapQuestXmlParser;


import android.location.LocationManager;
import android.os.AsyncTask;


public class LoadAltitudeFromNetwork {
	private AugmentedActivity aa;
	// Uploads XML from stackoverflow.com, parses it, and combines it with
    // HTML markup. Returns HTML string.
    public  void updateAltitude(String urlString, AugmentedActivity aa)  {
    	this.aa = aa;
    	new DownloadXmlTask().execute(urlString);
    }
    
    
    private  Double loadAltitudeFromXML(String urlString)throws XmlPullParserException, IOException{
    	InputStream stream = null;
        try {
    		MapQuestXmlParser maQuestXmlParser = new MapQuestXmlParser();;
            stream = downloadUrl(urlString);
            return maQuestXmlParser.readAltitude(stream);
        }
        finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    // Given a string representation of a URL, sets up a connection and gets
    // an input stream.
    private  InputStream downloadUrl(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000 /* milliseconds */);
        conn.setConnectTimeout(15000 /* milliseconds */);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        // Starts the query
        conn.connect();
        InputStream stream = conn.getInputStream();
        return stream;
    }
    
    
 // Implementation of AsyncTask used to download XML feed from stackoverflow.com.
    private class DownloadXmlTask extends AsyncTask<String, Void, Double> {


    	@Override
		protected Double doInBackground(String... params) {
    		try {
				return loadAltitudeFromXML(params[0]);
			} catch (XmlPullParserException e) {
				// TODO Auto-generated catch block
				return null;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				return null;
			}
           
		}
    	
    	@Override
    	protected void onPostExecute(Double result) {
    		try{
    			aa.setAltitude(result.doubleValue());	
    			aa.getCurrentLocation().setProvider(LocationManager.NETWORK_PROVIDER);
    		}catch(Exception e){}
			//aa.updateUI();
    	}
    	
    	

    }
}
