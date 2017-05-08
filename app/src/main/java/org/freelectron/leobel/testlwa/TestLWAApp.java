package org.freelectron.leobel.testlwa;

import android.support.multidex.MultiDexApplication;

/**
 * Created by leobel on 2/17/17.
 */

public class TestLWAApp extends MultiDexApplication {

    public ApplicationComponent component;

    private static TestLWAApp instance;

    public TestLWAApp(){instance = this;}

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Dagger component
        component = DaggerApplicationComponent
                .builder()
                .appModule(new AppModule(this))
                .servicesModule(new ServicesModule())
                .build();
    }

    public ApplicationComponent getComponent() {
        return component;
    }

    public static TestLWAApp getInstance(){
        return instance;
    }
}