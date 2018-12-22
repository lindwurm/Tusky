package jp.kyori.tusky;

import android.content.Context;
import android.os.Handler;
import android.text.Spanned;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.keylesspalace.tusky.appstore.EventHub;
import com.keylesspalace.tusky.appstore.StatusDeletedEvent;
import com.keylesspalace.tusky.appstore.StreamUpdateEvent;
import com.keylesspalace.tusky.entity.Status;
import com.keylesspalace.tusky.entity.StreamEvent;
import com.keylesspalace.tusky.json.SpannedTypeAdapter;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class TimelineStreamingClient extends WebSocketClient {

    private Context context;
    private Handler handler;
    private EventHub eventHub;

    TimelineStreamingClient(Context context, URI uri, EventHub eventHub) {
        super(uri);
        this.context = context;
        handler = new Handler(context.getMainLooper());
        this.eventHub = eventHub;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        showToast("Stream Connected.");
    }

    @Override
    public void onMessage(String message) {
        Gson gson = getGson();
        StreamEvent event = gson.fromJson(message, StreamEvent.class);

        String payload = event.getPayload();
        switch (event.getEvent()){
            case UPDATE:
                Status status = gson.fromJson(payload, Status.class);
                eventHub.dispatch(new StreamUpdateEvent(status));
                break;
            case DELETE:
                eventHub.dispatch(new StatusDeletedEvent(payload));
        }
    }

    private Gson getGson(){
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Spanned.class, new SpannedTypeAdapter());
        return gsonBuilder.create();
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        showToast("Stream Closed.");
    }

    @Override
    public void onError(Exception ex) {
        Log.e("TimelineStreamingClient", "onError: ", ex);
    }

    private void showToast(String message) {
        Log.d("TimelineStreamingClient", message);
        handler.post(() -> Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_SHORT).show());
    }
}
