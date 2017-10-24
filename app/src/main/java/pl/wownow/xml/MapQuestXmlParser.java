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

package pl.wownow.xml;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class parses XML feeds from openmapquestapi.com
 * Given an InputStream representation of a feed, it returns a List of entries,
 * where each list element represents a single entry (post) in the XML feed.
 */
public class MapQuestXmlParser {
    private static final String ns = null;

    // We don't use namespaces

    public Double readAltitude(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            int eventType = parser.getEventType();
	        while (eventType != XmlPullParser.END_DOCUMENT) {
	        	if(eventType == XmlPullParser.START_TAG) {
	        		if(parser.getName().equals("height")){
	        			return readAltitude(parser);
	        		}
	        	}
	        	eventType = parser.next();
	        }
        } finally {
            in.close();
        }
        
        return null;
    }

    
    // Processes title tags in the feed.
    private Double readAltitude(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "height");
        Double altitude = readDouble(parser);
        parser.require(XmlPullParser.END_TAG, ns, "height");
        return altitude;
    }
    // For the tags title and summary, extracts their text values.
    private Double readDouble(XmlPullParser parser) throws IOException, XmlPullParserException {
        Double result = null;
        if (parser.next() == XmlPullParser.TEXT) {
            result = Double.valueOf(parser.getText());
            parser.nextTag();
        }
        return result;
    }
   
}
