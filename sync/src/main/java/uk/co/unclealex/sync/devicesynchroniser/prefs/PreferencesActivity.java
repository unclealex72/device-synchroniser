package uk.co.unclealex.sync.devicesynchroniser.prefs;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.View;
import lombok.Getter;
import lombok.Setter;
import uk.co.unclealex.sync.devicesynchroniser.sync.R;


public class PreferencesActivity extends Activity {

    @Getter
    @Setter
    private ContentPreference contentPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (resultCode == RESULT_OK) {
            getContentPreference().updatePreference(resultData);
        }
    }

    public ContentPreference createContentPreference() {
        return new ContentPreference();
    }

    public class ContentPreference extends Preference {

        public ContentPreference() {
            super(PreferencesActivity.this);
        }

        @Override
        protected void onClick() {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.setFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, 0);
        }

        public void updatePreference(Intent resultData) {
            Uri treeUri = resultData.getData();
            getContentResolver().takePersistableUriPermission(treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION |
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            persistString(treeUri.toString());
        }
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            //EditTextPreference preference = new EditTextPreference(getActivity());
            PreferencesActivity activity = (PreferencesActivity) getActivity();
            Preference preference;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                preference = activity.createContentPreference();
                activity.setContentPreference((ContentPreference) preference);
            } else {
                preference = new EditTextPreference(getActivity());
            }
            preference.setSummary(R.string.pref_root_dir_summ);
            preference.setTitle(R.string.pref_root_dir);
            preference.setKey("pref_root_dir");

            this.getPreferenceScreen().addPreference(preference);
        }
    }
}
