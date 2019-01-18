package jp.kyori.tusky;

import android.text.Spanned;
import android.util.Log;

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

    private EventHub eventHub;

    private boolean isFirstStatus = true;

    public TimelineStreamingClient(URI uri, EventHub eventHub) {
        super(uri);
        this.eventHub = eventHub;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.d("TimelineStreamingClient", "onOpen: Stream Connected.");
    }

    @Override
    public void onMessage(String message) {
        Gson gson = getGson();
        StreamEvent event = gson.fromJson(message, StreamEvent.class);

        String payload = event.getPayload();
        switch (event.getEvent()) {
            case UPDATE:
                Status status = gson.fromJson(payload, Status.class);
                eventHub.dispatch(new StreamUpdateEvent(status, isFirstStatus));
                isFirstStatus = false;
                break;
            case DELETE:
                eventHub.dispatch(new StatusDeletedEvent(payload));
        }
    }

    private Gson getGson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Spanned.class, new SpannedTypeAdapter());
        return gsonBuilder.create();
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.d("TimelineStreamingClient", "onClose: Stream Closed.");
    }

    @Override
    public void onError(Exception ex) {
        Log.e("TimelineStreamingClient", "onError: ", ex);
    }
}
