package com.sam_chordas.android.stockhawk.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TimeUtils;
import android.widget.Toast;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Date;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService{
	private String LOG_TAG = StockTaskService.class.getSimpleName();
	public static final String BRODCAST_STRING="com.sam_chordas.android.stockhawk.app.ACTION_DATA_UPDATED";
	private OkHttpClient client = new OkHttpClient();
	private Context mContext;
	private StringBuilder mStoredSymbols = new StringBuilder();
	private boolean isUpdate;

	public StockTaskService(){}

	public StockTaskService(Context context){
		mContext = context;
	}
	String fetchData(String url) throws IOException{
		Request request = new Request.Builder()
				.url(url)
				.build();

		Response response = client.newCall(request).execute();
		return response.body().string();
	}

	@Override
	public int onRunTask(TaskParams params){
		Cursor initQueryCursor;
		String endDate="";
		if (mContext == null){
			mContext = this;
		}
		StringBuilder urlStringBuilder = new StringBuilder();
		boolean isHistoricalDataRequest=params.getTag().equals("historicalData");
		try{
			// Base URL for the Yahoo query
			urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
			if(isHistoricalDataRequest)
			{
				Cursor cursor=mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
						new String[]{QuoteColumns.HISTORICAL_JSON,QuoteColumns.HISTORICAL_JSON_LAST_UPDATE_DATE},
						QuoteColumns.ISCURRENT + " = ? and "+QuoteColumns.SYMBOL+" = ?",
						new String[]{"1",params.getExtras().getString("symbol")},
						null);
				if(cursor!=null) cursor.moveToFirst();
				SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd");
				Calendar calendar=Calendar.getInstance();
				endDate=simpleDateFormat.format(calendar.getTime());
				if(cursor!=null && cursor.getString(cursor.getColumnIndex(QuoteColumns.HISTORICAL_JSON))!=null &&
						!cursor.getString(cursor.getColumnIndex(QuoteColumns.HISTORICAL_JSON)).equals("") &&
						endDate.equals(cursor.getString(cursor.getColumnIndex(QuoteColumns.HISTORICAL_JSON_LAST_UPDATE_DATE))))
				{
					return GcmNetworkManager.RESULT_SUCCESS;
				}
				else
				{
					calendar.add(Calendar.DAY_OF_YEAR,-30);
					String startDate=simpleDateFormat.format(calendar.getTime());
					urlStringBuilder.append((URLEncoder.encode("select * from yahoo.finance.historicaldata where symbol = \""
							+params.getExtras().getString("symbol")+"\" and startDate = \""+startDate+
							"\" and endDate = \""+endDate+"\"","UTF-8")));
				}
			}
			else
			{
				urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.quotes where symbol in (", "UTF-8"));
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		if (isHistoricalDataRequest==false && (params.getTag().equals("init") || params.getTag().equals("periodic"))){
			isUpdate = true;
			initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
					new String[] { "Distinct " + QuoteColumns.SYMBOL }, null,
					null, null);
			if (initQueryCursor.getCount() == 0 || initQueryCursor == null){
				// Init task. Populates DB with quotes for the symbols seen below
				try {
					urlStringBuilder.append(
							URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			} else if (initQueryCursor != null){
				DatabaseUtils.dumpCursor(initQueryCursor);
				initQueryCursor.moveToFirst();
				for (int i = 0; i < initQueryCursor.getCount(); i++){
					mStoredSymbols.append("\""+
							initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol"))+"\",");
					initQueryCursor.moveToNext();
				}
				mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
				try {
					urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		} else if (params.getTag().equals("add")){
			isUpdate = false;
			// get symbol from params.getExtra and build query
			String stockInput = params.getExtras().getString("symbol");
			try {
				urlStringBuilder.append(URLEncoder.encode("\""+stockInput+"\")", "UTF-8"));
			} catch (UnsupportedEncodingException e){
				e.printStackTrace();
			}
		}
		// finalize the URL for the API query.
		urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
				+ "org%2Falltableswithkeys&callback=");

		String urlString;
		String getResponse;
		int result = GcmNetworkManager.RESULT_FAILURE;

		if (urlStringBuilder != null){
			urlString = urlStringBuilder.toString();
			try{
				getResponse = fetchData(urlString);
				result = GcmNetworkManager.RESULT_SUCCESS;
				if(isHistoricalDataRequest)
				{
					//TODO :getresponse is the json
					ContentValues contentValues = new ContentValues();
					contentValues.put(QuoteColumns.HISTORICAL_JSON,Utils.getQuoteString(getResponse));
					contentValues.put(QuoteColumns.HISTORICAL_JSON_LAST_UPDATE_DATE,
						endDate);
					mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI,
							contentValues,QuoteColumns.SYMBOL+"= ?",
							new String[]{params.getExtras().getString("symbol")});
				}
				else
				{
					try {
						ContentValues contentValues = new ContentValues();
						// update ISCURRENT to 0 (false) so new data is current
						if (isUpdate){
							contentValues.put(QuoteColumns.ISCURRENT, 0);
							contentValues.put(QuoteColumns.HISTORICAL_JSON_LAST_UPDATE_DATE,endDate);
							mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
									null, null);
						}
						mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
								Utils.quoteJsonToContentVals(getResponse,mContext));
						SendBroadcastDataChanged();
						SharedPreferences sharedPreferences= PreferenceManager.getDefaultSharedPreferences(mContext);
						SharedPreferences.Editor editor=sharedPreferences.edit();
						editor.putBoolean("is_updated",true);
						editor.commit();
					}catch (RemoteException | OperationApplicationException e){
						Log.e(LOG_TAG, "Error applying batch insert", e);
					}
				}
			} catch (IOException e){
				e.printStackTrace();
			}
		}
		return result;
	}
	private void SendBroadcastDataChanged()
	{
		Intent intent=new Intent(BRODCAST_STRING);
		mContext.sendBroadcast(intent);
	}
}
