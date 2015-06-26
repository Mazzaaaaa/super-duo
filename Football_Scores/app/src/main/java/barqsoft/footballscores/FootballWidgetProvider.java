package barqsoft.footballscores;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;

import barqsoft.footballscores.service.FootballIntentService;

/**
 * Created by Matteo on 25/06/2015.
 */
public class FootballWidgetProvider extends AppWidgetProvider {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // Is the intent an UPDATE_VIEW intent? Then update the widget
        if (action != null && action.equals(FootballIntentService.ACTION_UPDATE_VIEWS)) {
            context.startService(new Intent(context, FootballWidgetIntentService.class));
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        context.startService(new Intent(context, FootballWidgetIntentService.class));
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        context.startService(new Intent(context, FootballWidgetIntentService.class));
    }

    // This is the intent that updates the widget view
    public static class FootballWidgetIntentService extends IntentService {

        public FootballWidgetIntentService() {
            super("FootballWidgetIntentService");
        }

        @Override
        protected void onHandleIntent(Intent intent) {

            // Retrieve all of the Today widget ids: these are the widgets we need to update
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this,
                    FootballWidgetProvider.class));

            // Execute a query to obtain the info for the widget
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String currentDate = simpleDateFormat.format(new Date(System.currentTimeMillis()));
            String[] selectionArgs = new String[]{currentDate};

            Cursor cursor = getContentResolver().query(
                    DatabaseContract.scores_table.buildScoreWithDate(),
                    null,
                    null,
                    selectionArgs,
                    null);

            if (cursor == null) {
                return;
            }

            if (!cursor.moveToFirst()) {
                cursor.close();
                return;
            }

            String home = cursor.getString(ScoresViewAdapter.COL_HOME);
            String away = cursor.getString(ScoresViewAdapter.COL_AWAY);
            String score = Utilies.getScores(this, cursor.getInt(ScoresViewAdapter.COL_HOME_GOALS),
                    cursor.getInt(ScoresViewAdapter.COL_AWAY_GOALS));

            // Perform this loop procedure for each widget that belongs to this provider
            for (int appWidgetId : appWidgetIds) {

                // Create an Intent to launch MainActivity
                Intent mainIntent = new Intent(this, MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, mainIntent, 0);

                // Get the layout for the App Widget and attach an on-click listener
                RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.football_widget);
                remoteViews.setOnClickPendingIntent(R.id.widget, pendingIntent);

                // Update the view info
                remoteViews.setImageViewResource(R.id.widget_home_image,
                        Utilies.getTeamCrestByTeamName(this, home));
                remoteViews.setImageViewResource(R.id.widget_away_image,
                        Utilies.getTeamCrestByTeamName(this, away));
                remoteViews.setTextViewText(R.id.widget_home_text, home);
                remoteViews.setTextViewText(R.id.widget_away_text, away);
                remoteViews.setTextViewText(R.id.widget_score, score);

                // Tell the AppWidgetManager to perform an update on the current app widget
                appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
            }
            cursor.close();
        }
    }
}