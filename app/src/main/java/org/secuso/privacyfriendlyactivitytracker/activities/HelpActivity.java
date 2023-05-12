
package org.secuso.privacyfriendlyactivitytracker.activities;

import android.os.Bundle;
import android.widget.ExpandableListView;

import org.secuso.privacyfriendlyactivitytracker.R;
import org.secuso.privacyfriendlyactivitytracker.helpers.ExpandableListAdapter;
import org.secuso.privacyfriendlyactivitytracker.helpers.HelpDataDump;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;


public class HelpActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        ExpandableListAdapter expandableListAdapter;
        HelpDataDump helpDataDump = new HelpDataDump(this);

        ExpandableListView generalExpandableListView = (ExpandableListView) findViewById(R.id.generalExpandableListView);

        LinkedHashMap<String, List<String>> expandableListDetail = helpDataDump.getDataGeneral();
        List<String> expandableListTitleGeneral = new ArrayList<String>(expandableListDetail.keySet());
        expandableListAdapter = new ExpandableListAdapter(this, expandableListTitleGeneral, expandableListDetail);
        generalExpandableListView.setAdapter(expandableListAdapter);

        overridePendingTransition(0, 0);
    }

    @Override
    protected int getNavigationDrawerID() {
        return R.id.menu_help;
    }
}
