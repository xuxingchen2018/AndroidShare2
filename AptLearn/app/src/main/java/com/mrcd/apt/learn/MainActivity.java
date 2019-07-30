package com.mrcd.apt.learn;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import com.mrcd.apt.annotation.BindView;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.main_text)
    TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewFinder.getInstance().inject(this);
        mTextView.setText("就看崩不崩~~~");
    }
}
