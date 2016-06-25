package com.sam_chordas.android.stockhawk.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.AndroidCharacter;
import android.util.Log;
import android.widget.CursorAdapter;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.QuoteCursorAdapter;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;
import com.sam_chordas.android.stockhawk.ui.StockDetailsActivity;

/**
 * Created by rajor on 23-Jun-16.
 */
public class RemoteViewFactory implements RemoteViewsService.RemoteViewsFactory{
	private Context mContext;
	private Intent mIntent;
	private Cursor mCursor;
	public RemoteViewFactory(Context applicationContext, Intent intent) {
		mContext=applicationContext;
		mIntent=intent;
		mCursor=null;
	}

	@Override
	public void onCreate() {

	}

	@Override
	public void onDataSetChanged() {
		if(mCursor!=null)
			mCursor.close();
		mCursor=mContext.getContentResolver().query( QuoteProvider.Quotes.CONTENT_URI,
				new String[]{ QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
						QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
				QuoteColumns.ISCURRENT + " = ?",
				new String[]{"1"},
				null);
	}

	@Override
	public void onDestroy() {
		if(mCursor!=null)
			mCursor.close();
	}

	@Override
	public int getCount() {
		return mCursor.getCount();
	}

	@Override
	public RemoteViews getViewAt(int position)
	{
		Intent intent=new Intent(mContext, StockDetailsActivity.class);
		RemoteViews remoteViews=new RemoteViews(mContext.getPackageName(), R.layout.list_item_quote);
			if(mCursor.moveToPosition(position))
			{
				remoteViews.setTextViewText(R.id.stock_symbol,mCursor.getString(mCursor.getColumnIndex("symbol")));
				remoteViews.setTextViewText(R.id.bid_price,mCursor.getString(mCursor.getColumnIndex("bid_price")));
				remoteViews.setContentDescription(R.id.stock_symbol,"Symbol is "+mCursor.getString(mCursor.getColumnIndex("symbol")));
				remoteViews.setContentDescription(R.id.bid_price,"bid price for "+
						mCursor.getString(mCursor.getColumnIndex("symbol"))+" is "
						+mCursor.getString(mCursor.getColumnIndex("bid_price")));
				int sdk = Build.VERSION.SDK_INT;
				if (mCursor.getInt(mCursor.getColumnIndex("is_up")) == 1){
					remoteViews.setTextColor(R.id.change, Color.GREEN);
				} else{
					remoteViews.setTextColor(R.id.change,Color.RED);
				}
				remoteViews.setTextViewText(R.id.change,mCursor.getString(mCursor.getColumnIndex("change")));
				remoteViews.setContentDescription(R.id.change,"stocks of "+
						mCursor.getString(mCursor.getColumnIndex("symbol"))+
						" have changed by "+mCursor.getString(mCursor.getColumnIndex("change")));
				intent.putExtra("symbol",mCursor.getString(mCursor.getColumnIndex("symbol")));

				remoteViews.setOnClickFillInIntent(R.id.stock_list_item,intent);
			}
		return remoteViews;
	}

	@Override
	public RemoteViews getLoadingView() {
		return null;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}
}
