package com.sam_chordas.android.stockhawk.widget;

import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;
import com.sam_chordas.android.stockhawk.ui.StockDetailsActivity;

/**
 * Implementation of App Widget functionality.
 */
public class StocksWidget extends AppWidgetProvider {

	static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
								int appWidgetId) {
		// Construct the RemoteViews object
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.stocks_widget);
		views.setEmptyView(R.id.widget_linear,R.id.ListViewEmpty);
		Intent headingIntent=new Intent(context, MyStocksActivity.class);
		PendingIntent pendingIntent=PendingIntent.getActivity(context,0,headingIntent,0);
		views.setOnClickPendingIntent(R.id.heading_widget,pendingIntent);
		Intent intent=new Intent(context,StockWidgetRemoteViewService.class);
		views.setRemoteAdapter(R.id.widget_linear,intent);
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
			Intent intentTemplate=new Intent(context,StockDetailsActivity.class);
			pendingIntent=TaskStackBuilder.create(context).addNextIntentWithParentStack(intentTemplate).getPendingIntent(0,PendingIntent.FLAG_UPDATE_CURRENT);
			views.setPendingIntentTemplate(R.id.widget_linear,pendingIntent);
		}
		appWidgetManager.updateAppWidget(appWidgetId, views);
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		// There may be multiple widgets active, so update all of them
		for (int appWidgetId : appWidgetIds) {
			updateAppWidget(context, appWidgetManager, appWidgetId);
		}
	}

	@Override
	public void onEnabled(Context context) {
		// Enter relevant functionality for when the first widget is created
	}

	@Override
	public void onDisabled(Context context) {
		// Enter relevant functionality for when the last widget is disabled
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		if(StockTaskService.BRODCAST_STRING.equals(intent.getAction()))
		{
			int[] ids=AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context.getPackageName(),StocksWidget.class.getName()));
			AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(ids,R.id.widget_linear);
		}
	}
}

