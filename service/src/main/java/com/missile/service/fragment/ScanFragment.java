package com.missile.service.fragment;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.missile.service.R;
import com.missile.service.dialog.ShowQRCodeDialog;

public class ScanFragment extends Fragment {
    private final static String TAG = "ScanServer";
    private TextView mTextView;
    private Button mStartBtn;
    private ShowQRCodeDialog dialog;

    public static ScanFragment newInstance() {
        ScanFragment fragment = new ScanFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public ScanFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_scan, container, false);

        mStartBtn = (Button) rootView.findViewById(R.id.btn_start);
        mStartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog = new ShowQRCodeDialog(getActivity());
                dialog.showDialog();
            }
        });
        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (dialog != null) {
            dialog.dismissDialog();
        }
    }
}
