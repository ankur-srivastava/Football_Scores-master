package barqsoft.footballscores;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

import barqsoft.footballscores.service.myFetchService;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class ScoreWidgetService extends IntentService {
    private static final String ACTION_WIDGET = "barqsoft.footballscores.action.ACTION_WIDGET";
    private static final String EXTRA_PARAM = "barqsoft.footballscores.extra.EXTRA_PARAM";
    private static final String TAG = ScoreWidgetService.class.getSimpleName();

    Handler mHandler;
    static AppWidgetManager mAppWidgetManager;
    static int mAppWidgetId;
    scoresAdapter mAdapter;
    String widget_home_name_val;
    String widget_away_name_val;
    String mDate = null;
    String mTime = null;
    int COL_HOME_GOALS_VAL;
    int COL_AWAY_GOALS_VAL;

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

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this,
                FootballAppWidget.class));


        //https://github.com/udacity/Advanced_Android_Development/blob/master/app/src/main/java/com/example/android/sunshine/app/widget/TodayWidgetIntentService.java
        //Step 1 - Get Cursor which will get the data from DB
        //Step 2 - Use Cursor to populate the Remote View

        // Get today's data from the ContentProvider
        getData("n2");

        // Perform this loop procedure for each Today widget
        // Extract the data from the Cursor
        //widget_home_name, widget_score_textview, widget_data_textview, widget_away_name

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(getPackageName(), R.layout.football_app_widget);

            String scores = Utilies.getScores(COL_HOME_GOALS_VAL, COL_AWAY_GOALS_VAL);

            views.setTextViewText(R.id.widget_home_name, widget_home_name_val);
            views.setTextViewText(R.id.widget_away_name, widget_away_name_val);
            views.setTextViewText(R.id.widget_score_textview, scores);
            views.setTextViewText(R.id.widget_data_textview, mDate);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    private void update_scores(Context context) {
        Intent service_start = new Intent(context, myFetchService.class);
        context.startService(service_start);
    }

    private void updateUI(){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                //Toast.makeText(getApplicationContext(), "In UI thread", Toast.LENGTH_SHORT).show();

                RemoteViews views = new RemoteViews(getApplicationContext().getPackageName(), R.layout.football_app_widget);


                // Instruct the widget manager to update the widget
                mAppWidgetManager.updateAppWidget(mAppWidgetId, views);
            }
        });
    }

    //Custom Code

    private void getData (String timeFrame) {
        Log.v(TAG, "In getData for "+timeFrame);
        final String BASE_URL = "http://api.football-data.org/alpha/fixtures";
        final String QUERY_TIME_FRAME = "timeFrame";

        Uri fetch_build = Uri.parse(BASE_URL).buildUpon().appendQueryParameter(QUERY_TIME_FRAME, timeFrame).build();

        HttpURLConnection m_connection = null;
        BufferedReader reader = null;
        String JSON_data = null;

        try {
            URL fetch = new URL(fetch_build.toString());
            m_connection = (HttpURLConnection) fetch.openConnection();
            m_connection.setRequestMethod("GET");
            m_connection.addRequestProperty("X-Auth-Token",getString(R.string.api_key));
            m_connection.connect();

            InputStream inputStream = m_connection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                return;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
            }
            if (buffer.length() == 0) {
                return;
            }
            JSON_data = buffer.toString();
        }
        catch (Exception e){
            Log.e(TAG,"Exception here" + e.getMessage());
        }
        finally {
            if(m_connection != null){
                m_connection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException e)
                {
                    Log.e(TAG,"Error Closing Stream");
                }
            }
        }
        try {
            if (JSON_data != null) {
                Log.v(TAG, "JSON_data from service is "+JSON_data);
                JSONArray matches = new JSONObject(JSON_data).getJSONArray("fixtures");
                if (matches.length() == 0) {
                    processJSONdata(getString(R.string.dummy_data), getApplicationContext(), false);
                    return;
                }
                processJSONdata(JSON_data, getApplicationContext(), true);
            } else {
                Log.d(TAG, "Could not connect to server.");
            }
        }
        catch(Exception e)
        {
            Log.e(TAG,e.getMessage());
        }
    }
    private void processJSONdata (String JSONdata,Context mContext, boolean isReal) {
        final String BUNDESLIGA1 = "394";
        final String BUNDESLIGA2 = "395";
        final String LIGUE1 = "396";
        final String LIGUE2 = "397";
        final String PREMIER_LEAGUE = "398";
        final String PRIMERA_DIVISION = "399";
        final String SEGUNDA_DIVISION = "400";
        final String SERIE_A = "401";
        final String PRIMERA_LIGA = "402";
        final String Bundesliga3 = "403";
        final String EREDIVISIE = "404";

        //added by Ankur
        final String EL1 = "425";


        final String SEASON_LINK = "http://api.football-data.org/alpha/soccerseasons/";
        final String MATCH_LINK = "http://api.football-data.org/alpha/fixtures/";
        final String FIXTURES = "fixtures";
        final String LINKS = "_links";
        final String SOCCER_SEASON = "soccerseason";
        final String SELF = "self";
        final String MATCH_DATE = "date";
        final String HOME_TEAM = "homeTeamName";
        final String AWAY_TEAM = "awayTeamName";
        final String RESULT = "result";
        final String HOME_GOALS = "goalsHomeTeam";
        final String AWAY_GOALS = "goalsAwayTeam";
        final String MATCH_DAY = "matchday";

        //Match data
        String League = null;
        String match_id = null;


        try {
            JSONArray matches = new JSONObject(JSONdata).getJSONArray(FIXTURES);

            int i=0;
            //for(int i = 0;i < matches.length();i++) {
                JSONObject match_data = matches.getJSONObject(i);
                League = match_data.getJSONObject(LINKS).getJSONObject(SOCCER_SEASON).getString("href");
                League = League.replace(SEASON_LINK,"");
                if(League.equals(PREMIER_LEAGUE) || League.equals(SERIE_A) || League.equals(BUNDESLIGA1) || League.equals(BUNDESLIGA2) || League.equals(PRIMERA_DIVISION) || League.equals(EL1)){
                    match_id = match_data.getJSONObject(LINKS).getJSONObject(SELF).getString("href");
                    match_id = match_id.replace(MATCH_LINK, "");
                    if(!isReal){
                        match_id=match_id+Integer.toString(i);
                    }

                    mDate = match_data.getString(MATCH_DATE);
                    mTime = mDate.substring(mDate.indexOf("T") + 1, mDate.indexOf("Z"));
                    mDate = mDate.substring(0,mDate.indexOf("T"));
                    SimpleDateFormat match_date = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss");
                    match_date.setTimeZone(TimeZone.getTimeZone("UTC"));
                    try {
                        Date parseddate = match_date.parse(mDate+mTime);
                        SimpleDateFormat new_date = new SimpleDateFormat("yyyy-MM-dd:HH:mm");
                        new_date.setTimeZone(TimeZone.getDefault());
                        mDate = new_date.format(parseddate);
                        mTime = mDate.substring(mDate.indexOf(":") + 1);
                        mDate = mDate.substring(0,mDate.indexOf(":"));

                        if(!isReal){
                            //This if statement changes the dummy data's date to match our current date range.
                            Date fragmentdate = new Date(System.currentTimeMillis()+((i-2)*86400000));
                            SimpleDateFormat mformat = new SimpleDateFormat("yyyy-MM-dd");
                            mDate=mformat.format(fragmentdate);
                        }
                    }
                    catch (Exception e){
                        Log.d(TAG, "error here!");
                    }
                    widget_home_name_val = match_data.getString(HOME_TEAM);
                    widget_away_name_val = match_data.getString(AWAY_TEAM);
                    COL_HOME_GOALS_VAL = Integer.parseInt(match_data.getJSONObject(RESULT).getString(HOME_GOALS));
                    COL_AWAY_GOALS_VAL = Integer.parseInt(match_data.getJSONObject(RESULT).getString(AWAY_GOALS));

                    Log.v(TAG, "Values - "+widget_home_name_val+" "+widget_away_name_val+" "+COL_HOME_GOALS_VAL+" "+COL_AWAY_GOALS_VAL+" "+mDate);
                }
            //}

        }
        catch (JSONException e){
            Log.e(TAG,e.getMessage());
        }
    }
}
