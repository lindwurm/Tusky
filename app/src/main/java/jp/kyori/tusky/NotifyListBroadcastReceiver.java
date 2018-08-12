package jp.kyori.tusky;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class NotifyListBroadcastReceiver extends BroadcastReceiver {
    Handler handler;

    public NotifyListBroadcastReceiver(Handler h){
        super();
        handler=h;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Message message=new Message();
        Bundle bundle=new Bundle();
        if(intent.getExtras()!=null) {
            bundle.putSerializable("list", intent.getExtras().getSerializable("list"));
        }
        message.setData(bundle);
        handler.sendMessage(message);
    }
}
