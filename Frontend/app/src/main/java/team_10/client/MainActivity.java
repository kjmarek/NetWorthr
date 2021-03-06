package team_10.client;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;

import java.io.IOException;
import java.net.Socket;

import team_10.client.dashboard.DashboardFragment;
import team_10.client.data.source.AccountsRepository;
import team_10.client.data.source.UserRepository;
import team_10.client.login_and_registration.LoginActivity;
import team_10.client.news.NewsArticle;
import team_10.client.news.NewsFragment;
import team_10.client.settings.SettingsFragment;
import team_10.client.utility.io.AppExecutors;
import team_10.client.utility.io.SharedPreferencesManager;

import static android.support.v4.app.FragmentManager.POP_BACK_STACK_INCLUSIVE;

/**
 * This activity loads the dashboard fragment at start but also allows the user to switch to the news and settings fragments.
 */
public class MainActivity extends AppCompatActivity implements DashboardFragment.OnFragmentInteractionListener, NewsFragment.OnFragmentInteractionListener, NewsArticle.OnFragmentInteractionListener, SettingsFragment.OnFragmentInteractionListener {

    public static Context myContext;
    public static Socket socket;
    public String currentFragment;
    public int firstLoad;

    public static AccountsRepository mAccountsRepository;
    public static UserRepository mUserRepository;
    public static AppExecutors mAppExecutors;

    private static BottomNavigationView bottomNav;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            Fragment f = null;
            boolean worked = false;

            switch (item.getItemId()) {
                case R.id.navigation_news:
                    f = new NewsFragment();
                    worked = loadFragment(f, "news");
                    currentFragment = "news";
                    break;
                case R.id.navigation_dashboard:
                    f = new DashboardFragment();
                    worked = loadFragment(f, "home");
                    currentFragment = "home";
                    break;
                case R.id.navigation_settings:
                    f = new SettingsFragment();
                    worked = loadFragment(f, "settings");
                    currentFragment = "settings";
            }

            return worked;
        }
    };

    public static void hideBottomNavigationView() {
        bottomNav.setVisibility(View.GONE);
    }

    public static void showBottomNavigationView() {
        bottomNav.setVisibility(View.VISIBLE);
    }

    /* USE THIS FOR LOGIN / MAIN SCREEN FLOW AND INITS */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myContext = MainActivity.this;

        socket = new Socket();

        mAppExecutors = new AppExecutors();

        /* Checked logged in state. Finish activity if not logged in, otherwise, *
         * load the user's accounts and user data.                               */
        if (!SharedPreferencesManager.getInstance(this).isLoggedIn()) {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            finish();
        } else {
            mUserRepository = UserRepository.getInstance();
            mAccountsRepository = AccountsRepository.getInstance();

//            mAccountsRepository.deleteAllAccounts();
//            mUserRepository.removeUserData();
        }
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        bottomNav = navigation;
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        firstLoad = 0;
        navigation.setSelectedItemId(R.id.navigation_dashboard);
        //loadFragment(new DashboardFragment(), "home");
    }

    @Override
    protected void onPause() {
        super.onPause();

        try {
            socket.close();
        } catch (IOException e) {

            System.out.println("ERROR: Did not close socket.");

            e.printStackTrace();
        }
    }

    public boolean loadFragment(Fragment fragment, String name) {
        //switching fragment
        //fragment.getId();
        if (fragment != null && name != currentFragment) {
            final FragmentManager fragmentManager = getSupportFragmentManager();
            if (name.equals("home") && firstLoad == 1)
            {
                fragmentManager.popBackStack("settings", POP_BACK_STACK_INCLUSIVE);
                fragmentManager.popBackStack("news", POP_BACK_STACK_INCLUSIVE);
                bottomNav.getMenu().getItem(1).setChecked(true);
                return true;
            }
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragment);
            // 1. Know how many fragments there are in the stack
            final int count = fragmentManager.getBackStackEntryCount();
            // 2. If the fragment is **not** "home type", save it to the stack
            if ((name.equals("news") || name.equals("settings")) && count == 0) {
                fragmentTransaction.addToBackStack(name);
            }
            // Commit !
            fragmentTransaction.commit();
            // 3. After the commit, if the fragment is not an "home type" the back stack is changed, triggering the
            // OnBackStackChanged callback
            if (name.equals("home")) {
                firstLoad = 1;
                fragmentManager.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
                    @Override
                    public void onBackStackChanged() {
                        // If the stack decreases it means I clicked the back button
                        if (fragmentManager.getBackStackEntryCount() <= count) {
                            // pop all the fragment and remove the listener
                            fragmentManager.popBackStack("settings", POP_BACK_STACK_INCLUSIVE);
                            fragmentManager.popBackStack("news", POP_BACK_STACK_INCLUSIVE);
                            currentFragment = "home";
                            //fragmentManager.removeOnBackStackChangedListener(this);
                            // set the home button selected
                            bottomNav.getMenu().getItem(1).setChecked(true);
                        }
                    }
                });
            }
            return true;
        }
        if (fragment != null)
        {
            return true;
        }
        return false;
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
