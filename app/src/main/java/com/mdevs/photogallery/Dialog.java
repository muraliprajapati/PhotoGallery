package com.mdevs.photogallery;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;

/**
 * Created by Murali on 29-05-2015.
 */
public class Dialog extends DialogFragment {
    @NonNull
    @Override
    public android.app.Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog, null);
        return new AlertDialog.Builder(getActivity())
                .setTitle("Connection error")
                .setView(view)
                .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PhotoGalleryFragment fragment = (PhotoGalleryFragment) getFragmentManager().findFragmentById(R.id.fragmentContainer);
                        fragment.updateItems();

                    }
                }).create();
    }
}
