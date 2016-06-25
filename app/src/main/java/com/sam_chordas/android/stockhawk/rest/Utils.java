package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.camera2.params.Face;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {

  private static String LOG_TAG = Utils.class.getSimpleName();

  public static boolean showPercent = true;

  public static ArrayList quoteJsonToContentVals(String JSON, Context context){
    ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
    JSONObject jsonObject = null;
    JSONArray resultsArray = null;
    try{
	  jsonObject = new JSONObject(JSON);
      if (jsonObject != null && jsonObject.length() != 0){
        jsonObject = jsonObject.getJSONObject("query");
        int count = Integer.parseInt(jsonObject.getString("count"));
        if (count == 1){
          jsonObject = jsonObject.getJSONObject("results")
              .getJSONObject("quote");
				ContentProviderOperation contentProviderOperation=buildBatchOperation(jsonObject,context);
				if(contentProviderOperation!=null)
					batchOperations.add(contentProviderOperation);
        } else{
          resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

          if (resultsArray != null && resultsArray.length() != 0){
            for (int i = 0; i < resultsArray.length(); i++){
              jsonObject = resultsArray.getJSONObject(i);
				ContentProviderOperation contentProviderOperation=buildBatchOperation(jsonObject,context);
				if(contentProviderOperation!=null)
			  batchOperations.add(contentProviderOperation);
            }
          }
        }
      }
    } catch (JSONException e){
      Log.e(LOG_TAG, "String to JSON failed: " + e);
    }
    return batchOperations;
  }

  public static String truncateBidPrice(String bidPrice){
    bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
    return bidPrice;
  }

  public static String truncateChange(String change, boolean isPercentChange){
    String weight = change.substring(0,1);
    String ampersand = "";
    if (isPercentChange){
      ampersand = change.substring(change.length() - 1, change.length());
      change = change.substring(0, change.length() - 1);
    }
    change = change.substring(1, change.length());
    double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
    change = String.format("%.2f", round);
    StringBuffer changeBuffer = new StringBuffer(change);
    changeBuffer.insert(0, weight);
    changeBuffer.append(ampersand);
    change = changeBuffer.toString();
    return change;
  }
	public static String getQuoteString(String JSON){
		JSONObject jsonObject = null;
		JSONArray resultsArray = null;
		try{
			jsonObject = new JSONObject(JSON);
			if (jsonObject != null && jsonObject.length() != 0){
				jsonObject = jsonObject.getJSONObject("query");
				int count = Integer.parseInt(jsonObject.getString("count"));
					resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");
					return resultsArray.toString();
							}
		} catch (JSONException e){
			Log.e(LOG_TAG, "String to JSON failed: " + e);
		}
		return "";
	}
  public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject,Context context) throws JSONException {
	  if(jsonObject.getString("Bid")=="null" || jsonObject.getString("Bid")==null) {
		  SharedPreferences sharedPreferences= PreferenceManager.getDefaultSharedPreferences(context);
		  SharedPreferences.Editor editor=sharedPreferences.edit();
		  editor.putBoolean("status",true);
		  editor.commit();
		  return null;
	  }
	  	  ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
        QuoteProvider.Quotes.CONTENT_URI);
    try {
			String change = jsonObject.getString("Change");
			builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString("symbol"));
			builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString("Bid")));
			builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
					jsonObject.getString("ChangeinPercent"), true));
			builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
			builder.withValue(QuoteColumns.ISCURRENT, 1);
			if (change.charAt(0) == '-'){
				builder.withValue(QuoteColumns.ISUP, 0);
			}else {
				builder.withValue(QuoteColumns.ISUP, 1);
			}
    } catch (JSONException e){
      e.printStackTrace();
    }
    return builder.build();
  }
	public static String convertDateToMonthDay(String s)
	{
		DateFormat fromFormat=new SimpleDateFormat("yyyy-MM-dd");
		fromFormat.setLenient(false);
		DateFormat toFormat=new SimpleDateFormat("dd-MMM");
		toFormat.setLenient(false);
		try {
			Date date=fromFormat.parse(s);
			return toFormat.format(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return "";
	}
}
