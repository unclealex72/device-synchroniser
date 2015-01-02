package uk.co.unclealex.sync.devicesynchroniser.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.*;
import android.widget.TextView;
import uk.co.unclealex.sync.devicesynchroniser.BaseActivity;
import uk.co.unclealex.sync.devicesynchroniser.BaseFragment;
import uk.co.unclealex.sync.devicesynchroniser.changes.Changelog;
import uk.co.unclealex.sync.devicesynchroniser.prefs.PreferencesActivity;
import uk.co.unclealex.sync.devicesynchroniser.sync.R;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends BaseActivity implements MainView {

    @Inject
    MainPresenter mainPresenter;

    @Override
    protected List<Object> getModules() {
        return Arrays.<Object>asList(new MainModule(this));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            fragment.setArguments(new Bundle());
            getFragmentManager().beginTransaction()
                    .add(R.id.container, fragment)
                    .commit();
        }
    }

    @Override
    public TextView getLastSynchronisedTextView() {
        return (TextView) findViewById(R.id.last_synchronised_text_view);
    }

    @Override
    public TextView getPendingChangesTextView() {
        return (TextView) findViewById(R.id.pending_changes_text_view);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void onSynchroniseClicked(View view) {
        mainPresenter.synchronise();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, PreferencesActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends BaseFragment {

        @Inject
        MainPresenter mainPresenter;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            RecyclerView recList = (RecyclerView) rootView.findViewById(R.id.my_recycler_view);
            recList.setHasFixedSize(true);
            LinearLayoutManager llm = new LinearLayoutManager(getActivity());
            llm.setOrientation(LinearLayoutManager.VERTICAL);
            recList.setLayoutManager(llm);
            Changelog changelog = new Changelog();
            recList.setAdapter(new ChangelogItemAdapter(changelog, mainPresenter));
            LoadMoreData loadMoreData = new LoadMoreData(changelog, mainPresenter);
            recList.setOnScrollListener(loadMoreData);
            loadMoreData.updateWithExtra(recList);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

            SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                    AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void aVoid) {
                            updateGui();
                        }
                    };
                    task.execute();
                }
            };
            prefs.registerOnSharedPreferenceChangeListener(prefListener);
            return rootView;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            updateGui();
        }

        protected void updateGui() {
            mainPresenter.updateLastSynchronisedTime();
            mainPresenter.updatePendingChanges();
        }

    }

    public static class LoadMoreData extends RecyclerView.OnScrollListener {

        private final Changelog changelog;
        private final MainPresenter mainPresenter;

        private boolean loading = true;
        int pastVisiblesItems, visibleItemCount, totalItemCount;

        public LoadMoreData(Changelog changelog, MainPresenter mainPresenter) {
            this.changelog = changelog;
            this.mainPresenter = mainPresenter;
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            visibleItemCount = recyclerView.getLayoutManager().getChildCount();
            totalItemCount = recyclerView.getLayoutManager().getItemCount();
            pastVisiblesItems = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();

            if (loading) {
                if ((visibleItemCount + pastVisiblesItems) >= totalItemCount) {
                    updateWithExtra(recyclerView);
                }
            }
            super.onScrolled(recyclerView, dx, dy);
        }

        public void updateWithExtra(final RecyclerView recyclerView) {
            loading = false;
            AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        mainPresenter.loadNextChangelogPage(changelog);
                    } catch (IOException e) {
                        Log.e("main", e.getMessage());
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    recyclerView.getAdapter().notifyDataSetChanged();
                    loading = true;
                }
            };
            task.execute();
        }
    }
}
