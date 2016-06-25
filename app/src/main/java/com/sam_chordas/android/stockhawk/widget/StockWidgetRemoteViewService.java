package com.sam_chordas.android.stockhawk.widget;

import android.app.Service;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViewsService;

/**
 * Created by rajor on 23-Jun-16.
 */
public class StockWidgetRemoteViewService extends RemoteViewsService{
	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		return new RemoteViewFactory(this.getApplicationContext(),intent);
	}
}
