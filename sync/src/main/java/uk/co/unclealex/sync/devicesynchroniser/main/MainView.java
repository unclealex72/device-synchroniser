package uk.co.unclealex.sync.devicesynchroniser.main;

import android.widget.TextView;

/**
 * Created by alex on 21/12/14.
 */
public interface MainView {
    TextView getLastSynchronisedTextView();

    TextView getPendingChangesTextView();
}
