package net.eqozqq.mcpeproxy;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.widget.TabHost;

public class MainActivity extends Activity {

    public static final String TAB_TAG_HOME = "Home";
    public static final String TAB_TAG_SERVERS = "Servers";

    private TabHost tabHost;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_Holo_Light);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fragmentManager = getFragmentManager();

        tabHost = findViewById(android.R.id.tabhost);
        tabHost.setup();

        TabHost.TabSpec tabSpecHome = tabHost.newTabSpec(TAB_TAG_HOME);
        tabSpecHome.setIndicator(getString(R.string.tab_home));
        tabSpecHome.setContent(R.id.tab_home);
        tabHost.addTab(tabSpecHome);

        TabHost.TabSpec tabSpecServers = tabHost.newTabSpec(TAB_TAG_SERVERS);
        tabSpecServers.setIndicator(getString(R.string.tab_servers));
        tabSpecServers.setContent(R.id.tab_servers);
        tabHost.addTab(tabSpecServers);

        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                Fragment fragment = null;

                if (tabId.equals(TAB_TAG_HOME)) {
                    fragment = new HomeFragment();
                } else if (tabId.equals(TAB_TAG_SERVERS)) {
                    fragment = new ServersFragment();
                }

                if (fragment != null) {
                    FragmentTransaction ft = fragmentManager.beginTransaction();
                    ft.replace(android.R.id.tabcontent, fragment, tabId);
                    ft.commit();
                }
            }
        });

        tabHost.setCurrentTab(0);
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.replace(android.R.id.tabcontent, new HomeFragment(), TAB_TAG_HOME);
        ft.commit();
    }

    public void switchToTab(String tabTag) {
        if (tabHost != null) {
            tabHost.setCurrentTabByTag(tabTag);
        }
    }
}