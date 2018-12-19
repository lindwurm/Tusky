package jp.kyori.tusky;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import com.keylesspalace.tusky.ComposeActivity;
import com.keylesspalace.tusky.R;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class EditTextDialogFragment extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity());
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_edittext);
        EditText editText = dialog.findViewById(R.id.dialog_edittext);
        Button button = dialog.findViewById(R.id.dialog_button);
        button.setOnClickListener(view -> {
            String string = editText.getText().toString();
            ComposeActivity activity = (ComposeActivity) getActivity();
            activity.returnEditTextValue(string);
            dismiss();
        });
        return dialog;
    }
}
