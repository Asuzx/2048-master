package tk.woppo.mgame.tools;

import android.app.Application;
import android.content.Context;

/**
 * Created by lqh on 2017/4/22 0022.
 */

public class MyApplication extends Application {
    private static Context instance;

    public static Context getContext() {
        return context;
    }

    private static Context context;

    public static Context getInstance() {
        if (instance == null) {
            instance = new MyApplication();
        }
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.context=getApplicationContext();
        CrashHandler.getInstance().init(getApplicationContext());
    }

}
