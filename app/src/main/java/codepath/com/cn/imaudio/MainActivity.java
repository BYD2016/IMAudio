package codepath.com.cn.imaudio;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.btnRecordAudioByFile)
    void recordAudioByFile() {
        startActivity(new Intent(this, RecordAudioByFileActivity.class));
    }

    @OnClick(R.id.btnRecordAudioByStream)
    void recordAudioByStream() {
        startActivity(new Intent(this, RecordAudioByStreamActivity.class));
    }

}
