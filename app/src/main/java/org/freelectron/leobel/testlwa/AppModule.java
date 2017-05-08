package org.freelectron.leobel.testlwa;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by leobel on 2/17/17.
 */

@Module
public class AppModule {

    private Application app;

    public AppModule(Application app) {
        this.app = app;
    }

    @Singleton
    @Provides
    public Application providesApplication(){ return app; }

    @Singleton @Provides
    public SharedPreferences providesSharedPreference(Application app) {
        return app.getSharedPreferences(app.getPackageName(), Context.MODE_PRIVATE);
    }
}
