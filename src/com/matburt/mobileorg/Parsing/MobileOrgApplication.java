package com.matburt.mobileorg.Parsing;

import android.app.Application;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.matburt.mobileorg.Services.CalendarSyncService;
import com.matburt.mobileorg.Services.SyncService;

public class MobileOrgApplication extends Application {
    private OrgDatabase appdb;
	private CalendarSyncService calendarSyncService;
    
    @Override
    public void onCreate() {
    	this.appdb = new OrgDatabase(getApplicationContext());
		init();
		SyncService.startAlarm(getApplicationContext());
    }
    
    private void init() {
		if (this.appdb == null || this.appdb.getFiles().isEmpty())
			return;
	}
  
    
    public OrgDatabase getDB() {
    	return this.appdb;
    }
    
    public CalendarSyncService getCalendarSyncService() {
    	if(this.calendarSyncService == null)
    		this.calendarSyncService = new CalendarSyncService(getDB(), this);
    	
    	return this.calendarSyncService;
    }
    
    public boolean isSyncConfigured() {
    	String syncSource = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
		.getString("syncSource", "");
    	
    	if(TextUtils.isEmpty(syncSource))
    		return false;
    	else
    		return true;
    }
}
