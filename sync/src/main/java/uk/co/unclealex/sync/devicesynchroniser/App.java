package uk.co.unclealex.sync.devicesynchroniser;

/**
 * Created by alex on 17/12/14.
 */

import android.app.Application;
import dagger.ObjectGraph;

import java.util.Arrays;
import java.util.List;

public class App extends Application {

    private ObjectGraph objectGraph;

    @Override
    public void onCreate() {
        super.onCreate();
        objectGraph = ObjectGraph.create(getModules().toArray());
        objectGraph.inject(this);
    }

    private List<Object> getModules() {
        return Arrays.<Object>asList(new AppModule(this));
    }

    public ObjectGraph createScopedGraph(Object... modules) {
        return objectGraph.plus(modules);
    }

    public ObjectGraph getObjectGraph() {
        return objectGraph;
    }
}