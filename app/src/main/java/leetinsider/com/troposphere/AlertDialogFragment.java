package leetinsider.com.troposphere;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;

/**
 * Created by FinalTrigger on 1/24/15.
 */
public class AlertDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //Context = current activity this fragment is called from
        Context context = getActivity();
        //Create new Alert Dialog Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.error_title))
                .setMessage(context.getString(R.string.error_message))
                .setPositiveButton(context.getString(R.string.error_okButton_text), null);

        AlertDialog dialog = builder.create();
        return dialog;
    }
}
