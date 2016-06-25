package com.sam_chordas.android.stockhawk.ui;

import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.db.chart.Tools;
import com.db.chart.model.LineSet;
import com.db.chart.view.ChartView;
import com.db.chart.view.LineChartView;
import com.db.chart.view.Tooltip;
import com.db.chart.view.animation.Animation;
import com.db.chart.view.animation.easing.BounceEase;
import com.db.chart.view.animation.easing.CircEase;
import com.db.chart.view.animation.easing.CubicEase;
import com.db.chart.view.animation.easing.ElasticEase;
import com.db.chart.view.animation.easing.ExpoEase;
import com.db.chart.view.animation.easing.LinearEase;
import com.db.chart.view.animation.easing.QuadEase;
import com.db.chart.view.animation.easing.QuartEase;
import com.db.chart.view.animation.easing.QuintEase;
import com.db.chart.view.animation.easing.SineEase;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteDatabase;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class StockDetailsActivity extends AppCompatActivity implements android.app.LoaderManager.LoaderCallbacks<Cursor>{

	private  String symbol="";
	private  static int CURSOR_LOADER_ID=0;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle extras=getIntent().getExtras();
		symbol=extras.getString("symbol");
		setContentView(R.layout.stock_details_layout);
		Toolbar toolbar = (Toolbar) findViewById(R.id.details_toolbar);
		toolbar.setTitle(symbol);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		ConnectivityManager cm =
				(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);

		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		 boolean isConnected = activeNetwork != null &&
				activeNetwork.isConnectedOrConnecting();
		Intent mServiceIntent = new Intent(this, StockIntentService.class);
		if (savedInstanceState == null){
			mServiceIntent.putExtra("tag", "historicalData");
			mServiceIntent.putExtra("symbol",symbol);
			if (isConnected){
				startService(mServiceIntent);
			} else{
				networkToast();
			}
		}
		getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);
		}

	public void networkToast(){
		Toast.makeText(this, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
	}

	@Override
	public android.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
				new String[]{QuoteColumns.HISTORICAL_JSON},
				QuoteColumns.ISCURRENT + " = ? and "+QuoteColumns.SYMBOL+" = ?",
				new String[]{"1",symbol},
				null);
	}

	private void addDataToLineChart(LineChartView lineChartView,String Json) {
		try {
			ArrayList<String> labels=new ArrayList<String>();
			List<Float> values=new ArrayList<Float>();
			JSONObject jsonObject;
			JSONArray jsonArray=new JSONArray(Json);
			if (jsonArray != null && jsonArray.length() != 0){
				for (int i = 0; i < jsonArray.length(); i++){
					jsonObject = jsonArray.getJSONObject(i);
					labels.add(0,i%2==0? Utils.convertDateToMonthDay(jsonObject.getString("Date")):"");
					values.add(0,Float.valueOf(jsonObject.getString("Close")));
				}
			}
			float[] mValues=new float[values.size()];
			float min=Float.MAX_VALUE,max=Float.MIN_VALUE;
			for(int i=0;i<values.size();i++)
			{
				mValues[i] = values.get(i);
				min=min<mValues[i]?min:mValues[i];
				max=max>mValues[i]?max:mValues[i];
			}
			LineSet lineset=new LineSet(labels.toArray(new String[0]),mValues);
			lineset.setFill(Color.rgb(145,205,100));
			lineset.setDotsStrokeColor(Color.RED);
			lineset.setColor(Color.GRAY);
			lineChartView.addData(lineset);
			int mmax=(int)max+10;
			int mmin=(int)(min-10<0?0:min-10);
			mmax+=10-(mmax-mmin)%10;
			lineChartView.setAxisBorderValues(mmin,mmax,10);
			buildChart(lineChartView);
			lineChartView.show(buildAnimation());
		}
		catch (JSONException e)
		{
			Log.d("error handling json",e.toString());
		}
	}
	@Override
	public void onLoadFinished(android.content.Loader<Cursor> loader, Cursor data) {
		if(data==null || data.getCount()==0)
			return;
		LineChartView lineChartView=(LineChartView)findViewById(R.id.linechart);
		data.moveToFirst();
		lineChartView.reset();
		String json=data.getString(data.getColumnIndex(QuoteColumns.HISTORICAL_JSON));
		if(json==null)
			return;
		addDataToLineChart(lineChartView,json);
	}

	@Override
	public void onLoaderReset(android.content.Loader<Cursor> loader) {
	}

	private void buildChart(ChartView chart){

		Paint mGridPaint =  new Paint();
		mGridPaint.setColor(Color.GRAY);
		mGridPaint.setStyle(Paint.Style.STROKE);
		mGridPaint.setAntiAlias(true);
		mGridPaint.setStrokeWidth(Tools.fromDpToPx(1));
			chart.setGrid(ChartView.GridType.FULL, mGridPaint);
	}
	private static Animation buildAnimation(){
		return new Animation(1000)
				.setEasing(new ExpoEase())
				.setStartPoint(0.5f,0.0f);
	}

}
