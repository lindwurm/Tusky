package jp.kyori.tusky;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.Base64;

import com.keylesspalace.tusky.ComposeActivity;

import java.util.ArrayList;

public class NotificationPickDialogFragment extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        ArrayList<String[]> itemList = new ArrayList<>();
        String[] viewList = new String[0];
        if (getArguments() != null) {
            Object obj = getArguments().get("list");
            if (obj instanceof ArrayList<?>) {
                ArrayList<?> listPlaceHolder = (ArrayList<?>) obj;
                viewList = new String[listPlaceHolder.size()];
                for (int i = 0; i < listPlaceHolder.size(); i++) {
                    if (listPlaceHolder.get(i) instanceof String[]) {
                        try {
                            String[] stringsPlaceHolder = (String[]) listPlaceHolder.get(i);
                            itemList.add(stringsPlaceHolder);
                            viewList[i] = stringsPlaceHolder[0];
                        } catch (ClassCastException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        dialogBuilder.setTitle("Please select music notification.");
        dialogBuilder.setItems(viewList, (dialogInterface, i) -> {
            ComposeActivity activity = (ComposeActivity) getActivity();
            if (activity != null) {
                activity.addStringAfter("#NowPlaying\n" + itemList.get(i)[0] + "\n" + itemList.get(i)[1]);
                String imageStr = itemList.get(i)[2];
                if (!"".equals(imageStr)) {
                    byte[] imageBytes = Base64.decode(imageStr, 0);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                    activity.addCoverToQueue(ComposeActivity.QueuedMedia.Type.IMAGE, bitmap, imageBytes, bitmap.getByteCount());
                }
            }
        });

        return dialogBuilder.create();
    }
}
