package com.matburt.mobileorg;

import android.app.ListActivity;
import android.app.Application;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.ListView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.content.Intent;
import android.content.Context;
import android.util.Log;
import java.util.ArrayList;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class MobileOrgActivity extends ListActivity
{
    private static class OrgViewAdapter extends BaseAdapter {

        public Node topNode;
        public Node thisNode;
        public ArrayList<Integer> nodeSelection;
        private LayoutInflater lInflator;

        public OrgViewAdapter(Context context, Node ndx, ArrayList<Integer> selection) {
            this.topNode = ndx;
            this.thisNode = ndx;
            this.lInflator = LayoutInflater.from(context);
            this.nodeSelection = selection;
            Log.d("OVA", "Selection Stack");
            if (selection != null) {
                for (int idx = 0; idx < selection.size(); idx++) {
                    this.thisNode = this.thisNode.subNodes.get(
                                          selection.get(idx));
                    Log.d("OVA", this.thisNode.nodeName);
                }
            }
        }

        public int getCount() {
            return this.thisNode.subNodes.size();
        }

        /**
         * Since the data comes from an array, just returning the index is
         * sufficent to get at the data. If we were using a more complex data
         * structure, we would return whatever object represents one row in the
         * list.
         *
         * @see android.widget.ListAdapter#getItem(int)
         */
        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = this.lInflator.inflate(R.layout.main, null);
            }

            TextView thisView = (TextView)convertView.findViewById(R.id.orgItem);
            thisView.setText(this.thisNode.subNodes.get(position).nodeName);
            Log.d("MobileOrg", "Returning view item: " + this.thisNode.subNodes.get(position).nodeName);
            convertView.setTag(thisView);
            return convertView;
        }
    }

    private static final int OP_MENU_SETTINGS = 1;
    private static final int OP_MENU_SYNC = 2;
    private static final int OP_MENU_OUTLINE = 3;
    private static final int OP_MENU_CAPTURE = 4;
    private static final String LT = "MobileOrg";
    private Synchronizer appSync;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.initializeTables();
    }

    @Override
    public void onResume() {
        super.onResume();
        MobileOrgApplication appInst = (MobileOrgApplication)this.getApplication();
        if (appInst.rootNode == null) {
            ArrayList<String> allOrgList = this.getOrgFiles();
            OrgFileParser ofp = new OrgFileParser(allOrgList);
            ofp.parse();
            appInst.rootNode = ofp.rootNode;
        }

        Intent nodeIntent = getIntent();
        appInst.nodeSelection = nodeIntent.getIntegerArrayListExtra("nodePath");
        this.setListAdapter(new OrgViewAdapter(this,
                                               appInst.rootNode,
                                               appInst.nodeSelection));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MobileOrgActivity.OP_MENU_OUTLINE, 0, "Outline");
        menu.add(0, MobileOrgActivity.OP_MENU_CAPTURE, 0, "Capture");
        menu.add(0, MobileOrgActivity.OP_MENU_SYNC, 0, "Sync");
        menu.add(0, MobileOrgActivity.OP_MENU_SETTINGS, 0, "Settings");
        return true;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent dispIntent = new Intent();
        MobileOrgApplication appInst = (MobileOrgApplication)this.getApplication();
        Log.d("OVA", "Item selected was: " + position);
        dispIntent.setClassName("com.matburt.mobileorg",
                                "com.matburt.mobileorg.MobileOrgActivity");
        if (appInst.nodeSelection == null) {
            appInst.nodeSelection = new ArrayList<Integer>();
        }

        appInst.nodeSelection.add(new Integer(position));
        dispIntent.putIntegerArrayListExtra("nodePath", appInst.nodeSelection);
        startActivityForResult(dispIntent, 1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        MobileOrgApplication appInst = (MobileOrgApplication)this.getApplication();
        appInst.nodeSelection.remove(appInst.nodeSelection.size()-1);
    }

    public boolean onShowSettings() {
        Intent settingsIntent = new Intent();
        settingsIntent.setClassName("com.matburt.mobileorg",
                                    "com.matburt.mobileorg.SettingsActivity");
        startActivity(settingsIntent);
        return true;
    }

    /* Handles item selections */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MobileOrgActivity.OP_MENU_SYNC:
            appSync = new Synchronizer(this);
            appSync.pull();
            this.onResume();
            return true;
        case MobileOrgActivity.OP_MENU_SETTINGS:
            return this.onShowSettings();
        case MobileOrgActivity.OP_MENU_OUTLINE:
            return true;
        case MobileOrgActivity.OP_MENU_CAPTURE:
            return true;
        }
        return false;
    }

    public ArrayList<String> getOrgFiles() {
        ArrayList<String> allFiles = new ArrayList<String>();
        SQLiteDatabase appdb = this.openOrCreateDatabase("MobileOrg",
                                                         MODE_PRIVATE, null);
        Cursor result = appdb.rawQuery("SELECT file FROM files", null);
        if (result != null) {
            if (result.getCount() > 0) {
                result.moveToFirst();
                do {
                    Log.d(LT, "pulled " + result.getString(0));
                    allFiles.add(result.getString(0));
                } while(result.moveToNext());
            }
        }
        appdb.close();
        result.close();
        return allFiles;
        //return (String[])allFiles.toArray(new String[0]);
    }

    public void initializeTables() {
        SQLiteDatabase appdb = this.openOrCreateDatabase("MobileOrg",
                                                         MODE_PRIVATE, null);
        appdb.execSQL("CREATE TABLE IF NOT EXISTS settings"
                      + " (key VARCHAR, val VARCHAR)");
        appdb.execSQL("CREATE TABLE IF NOT EXISTS files"
                      + " (file VARCHAR, name VARCHAR,"
                      + " checksum VARCHAR);");
        appdb.close();

    }
}
