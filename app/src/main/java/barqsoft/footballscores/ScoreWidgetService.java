package barqsoft.footballscores;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class ScoreWidgetService extends IntentService {
    private static final String ACTION_WIDGET = "barqsoft.footballscores.action.ACTION_WIDGET";
    private static final String EXTRA_PARAM = "barqsoft.footballscores.extra.EXTRA_PARAM";
    private static final String TAG = ScoreWidgetService.class.getSimpleName();

    private Handler mHandler;
    private static AppWidgetManager mAppWidgetManager;
    private static int mAppWidgetId;

    public ScoreWidgetService() {
        super("ScoreWidgetService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        mHandler = new Handler();
        return super.onStartCommand(intent, flags, startId);
    }

    public static void startActionWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        mAppWidgetManager = appWidgetManager;
        mAppWidgetId = appWidgetId;
        Intent intent = new Intent(context, ScoreWidgetService.class);
        intent.setAction(ACTION_WIDGET);
        //intent.putExtra(EXTRA_PARAM, param);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v(TAG, "In onHandleIntent");
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_WIDGET.equals(action)) {
                final String param = intent.getStringExtra(EXTRA_PARAM);
            }
        }
        Log.v(TAG, "Call Main Thread");
        updateUI();
    }

    private void updateUI(){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "In UI thread", Toast.LENGTH_SHORT).show();
                // Construct the RemoteViews object
                RemoteViews views = new RemoteViews(getApplicationContext().getPackageName(), R.layout.football_app_widget);
                views.setTextViewText(R.id.appwidget_text, "Sample");

                // Instruct the widget manager to update the widget
                mAppWidgetManager.updateAppWidget(mAppWidgetId, views);
            }
        });
    }
}
