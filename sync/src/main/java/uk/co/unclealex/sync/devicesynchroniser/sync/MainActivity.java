package uk.co.unclealex.sync.devicesynchroniser.sync;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.TextView;
import org.bostonandroid.datepreference.DatePreference;
import org.bostonandroid.timepreference.TimePreference;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class MainActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final int PERMISSIONS_REQUEST_CODE = 42;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        //updateLastSynchronisedTime(sharedPreferences);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void onSynchroniseClicked(View view) {
        SynchroniseService.startAction(getBaseContext());
    }

    public void onPermissionsClicked(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.setFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PERMISSIONS_REQUEST_CODE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (resultCode == RESULT_OK) {
            Uri treeUri = resultData.getData();
            getContentResolver().takePersistableUriPermission(treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION |
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);

            // List all existing files inside picked directory
            for (DocumentFile file : pickedDir.listFiles()) {
                Log.d("TAG", "Found file " + file.getName() + " with size " + file.length());
            }

            // Create a new file and write into it
            DocumentFile newFile = pickedDir.createFile("text/plain", "My Novel");
            try {
                OutputStream out = getContentResolver().openOutputStream(newFile.getUri());
                out.write("A long time ago...".getBytes());
                out.close();
            }
            catch (IOException e) {
                Log.e("TAG", e.getMessage());
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updateLastSynchronisedTime(sharedPreferences);
    }

    protected void updateLastSynchronisedTime(SharedPreferences sharedPreferences) {
        String text;
        Calendar day = DatePreference.getDateFor(sharedPreferences, "pref_date_since");
        Calendar time = TimePreference.getTimeFor(day, sharedPreferences, "pref_time_since");
        if (time == null) {
            text = getString(R.string.never_synchronised);
        }
        else {
            DateFormat df = new SimpleDateFormat(getString(R.string.last_synchronised_date_format));
            String preamble = getString(R.string.last_synchronised_text);
            Date when = time.getTime();
            text = preamble + ": " + df.format(when);
        }
        TextView view = (TextView) findViewById(R.id.lastSynchronisedTextView);
        view.setText(text);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            return rootView;
        }
    }
}
