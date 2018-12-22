package jp.kyori.tusky;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.keylesspalace.tusky.appstore.EventHub;
import com.keylesspalace.tusky.db.AccountEntity;
import com.keylesspalace.tusky.db.AccountManager;
import com.keylesspalace.tusky.di.Injectable;

import org.java_websocket.WebSocketImpl;

import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class TimelineStreamingService extends Service implements Injectable {

    @Inject
    AccountManager accountManager;
    @Inject
    public EventHub eventHub;

    private TimelineStreamingClient streamingClient;

    @Override
    public void onCreate() {
        AndroidInjection.inject(this);

        connectWebsocket(buildStreamingUrl());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        streamingClient.close();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private String buildStreamingUrl() {
        AccountEntity activeAccount = accountManager.getActiveAccount();
        if (activeAccount != null) {
            return "wss://" + activeAccount.getDomain() + "/api/v1/streaming/?" + "stream=user" + "&" + "access_token" + "=" + activeAccount.getAccessToken();
        }else{
            return null;
        }
    }

    private void connectWebsocket(String endpoint) {
        try {
            streamingClient = new TimelineStreamingClient(this, new URI(endpoint), eventHub);
        } catch (URISyntaxException e) {
            Log.e("TimelineStreaming", "connectWebsocket: ", e);
            return;
        }

        streamingClient.connect();
    }

}
