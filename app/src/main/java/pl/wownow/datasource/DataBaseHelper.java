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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;


import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;



public class DataBaseHelper extends SQLiteOpenHelper {
	 //The Android's default system path of your application database.
	private static final String DB_PATH = "/data/data/pl.wownow.main/databases/";
	 
	private static final String DB_NAME = "peaks.db";
	 
	private SQLiteDatabase myDataBase = null;
	 
	private final Context myContext;
	 
	/**
	  * Constructor
	  * Takes and keeps a reference of the passed context in order to access to the application assets and resources.
	  * @param context
	  */
	public DataBaseHelper(Context context) {
	 
		super(context, DB_NAME, null, 1);
		this.myContext = context;
	}	
	 
	/**
	  * Creates a empty database on the system and rewrites it with your own database.
	  * */
	public synchronized void createDataBase() throws IOException{
	 
		boolean dbExist = checkDataBase();
		 
		if(dbExist){
			//do nothing - database already exist
		}else{
			//By calling this method and empty database will be created into the default system path
			//of your application so we are gonna be able to overwrite that database with our database.
			this.getReadableDatabase();
			 
			try {
				copyDataBase(); 
			} catch (IOException e) {e.printStackTrace();}
		}
	 
	}
	 
	/**
	  * Check if the database already exist to avoid re-copying the file each time you open the application.
	  * @return true if it exists, false if it doesn't
	  */
	private synchronized boolean checkDataBase(){
	 
		SQLiteDatabase checkDB = null;
		 
		try{
			String myPath = DB_PATH + DB_NAME;
			checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
		 
		}catch(SQLiteException e){ 
			//database does't exist yet.
		}catch(Exception e){}
		 
		finally{
			if(checkDB != null){
				try{
					checkDB.close();
				}catch(Exception e){}
			}
		}
		 
		return checkDB != null ? true : false;
	}
	 
	/**
	  * Copies your database from your local assets-folder to the just created empty database in the
	  * system folder, from where it can be accessed and handled.
	  * This is done by transfering bytestream.
	  * */
	private synchronized void copyDataBase() throws IOException{
	 
		//Open your local db as the input stream
		InputStream myInput = myContext.getAssets().open(DB_NAME);
		 
		// Path to the just created empty db
		String outFileName = DB_PATH + DB_NAME;
		 
		//Open the empty db as the output stream
		OutputStream myOutput = new FileOutputStream(outFileName);
		 
		//transfer bytes from the inputfile to the outputfile
		byte[] buffer = new byte[1024];
		int length;
		while ((length = myInput.read(buffer))>0){
			myOutput.write(buffer, 0, length);
		}
		 
		//Close the streams
		myOutput.flush();
		myOutput.close();
		myInput.close();
	 
	}
	 
	public synchronized void openDataBase() throws SQLException{
	 
		//Open the database
		String myPath = DB_PATH + DB_NAME;
		myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
	 
	}
	
	/**
	 * 
	 * @param center - current location around which all peaks should be fetched from database
	 * @param radiusInDergees - radius of the circle from which peaks should be fetched
	 * @return list of Peak objects
	 */
	public synchronized LinkedList<Peak> getPeaks(Location center, double radiusInDergees){
		LinkedList<Peak> list = new LinkedList<Peak>();	
		try{	
			if(myDataBase == null) return list;
			String query = "SELECT * FROM `peaks` WHERE `lat` <= "+(center.getLatitude()+1)+
					" AND `lat` >= "+(center.getLatitude()-1)+
					" AND `lon` <= "+(center.getLongitude()+1)+
					" AND `lon` >= "+(center.getLongitude()-1);
			
			Cursor c = myDataBase.rawQuery(query, null);
		
			
			if(c.moveToFirst()){
				do{
					String name = c.getString(c.getColumnIndex("name"));
					double lat = c.getDouble(c.getColumnIndex("lat"));
					double lon =c.getDouble(c.getColumnIndex("lon"));
					int ele = c.getInt(c.getColumnIndex("ele"));
					list.add(new Peak(name, lat, lon, ele));
				}while(c.moveToNext());
			}
					
			c.close();
		}catch(Exception e){}

		return list;
	}
	
	 
	@Override
	public synchronized void close() {
	 
		if(myDataBase != null)
		myDataBase.close();
		 
		super.close();
	 
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		
	}
	 
	
	// Add your public helper methods to access and get content from the database.
	// You could return cursors by doing "return myDataBase.query(....)" so it'd be easy
	// to you to create adapters for your views.
	 
	
}
