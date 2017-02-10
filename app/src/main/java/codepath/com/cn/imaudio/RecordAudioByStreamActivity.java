package codepath.com.cn.imaudio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import codepath.com.cn.imaudio.utils.RecordAudioUtils;
import codepath.com.cn.imaudio.utils.UiThreadUtils;

public class RecordAudioByStreamActivity extends AppCompatActivity {

    private static final String TAG = RecordAudioByStreamActivity.class.getSimpleName();
    private static final int BUFFER_SIZE = 2048;

    @BindView(R.id.tvLog)
    TextView mTvLog;
    @BindView(R.id.btnRecordAudio)
    Button mBtnRecordAudio;
    @BindView(R.id.btnPlayAudio)
    Button mBtnPlayAudio;

    private ExecutorService mExecutorService;

    private File mAudioFile;
    private long mBeginRecordInMillis, mEndRecordInMillis;

    private byte[] mBuffer;
    private FileOutputStream mFileOutputStream;
    private AudioRecord mAudioRecord;

    // 录音状态
    private volatile boolean mIsRecording = false;

    // 播放状态
    private volatile boolean mIsplaying = false;



    void setRecording(boolean recording) {
        mIsRecording = recording;

        mBtnRecordAudio.setText(
                mIsRecording ? R.string.record_audio_stop : R.string.record_audio_start);

        if (mIsRecording) {
            mExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    if (!doStartRecordAudio()) {
                        echoFail();
                    }
                }
            });
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_audio_by_stream);
        ButterKnife.bind(this);

        mExecutorService = Executors.newSingleThreadExecutor();

        mBuffer = new byte[BUFFER_SIZE];
    }

    @Override
    protected void onDestroy() {
        mExecutorService.shutdownNow();
        super.onDestroy();
    }

    @OnClick(R.id.btnRecordAudio)
    void startRecordAudio() {
        setRecording(!mIsRecording);
    }

    @OnClick(R.id.btnPlayAudio)
    void playAudioHandler() {

        if (! mIsplaying) {

            if (mAudioFile != null) {
                mExecutorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        doPlayAudio(mAudioFile);
                    }
                });
            } else {
                Toast.makeText(RecordAudioByStreamActivity.this, "请先录音...",
                        Toast.LENGTH_SHORT).show();
            }

        }
    }

    private boolean doStartRecordAudio() {
        // 创建录音文件
        if (!createAudioFile()) return false;

        try {
            mFileOutputStream = new FileOutputStream(mAudioFile);

            // 配置AudioRecord
            int audioSource = MediaRecorder.AudioSource.MIC;
            int simpleRateHz = 44100;
            int channelConfig = AudioFormat.CHANNEL_IN_MONO;
            int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
            int minBufferSize = AudioRecord.getMinBufferSize(simpleRateHz, channelConfig, audioFormat);

            mAudioRecord = new AudioRecord(audioSource, simpleRateHz, channelConfig, audioFormat
                    , Math.max(minBufferSize, BUFFER_SIZE));


            mAudioRecord.startRecording();
            mBeginRecordInMillis = System.currentTimeMillis();

            while (mIsRecording) {
                int readBytes = mAudioRecord.read(mBuffer, 0, BUFFER_SIZE);
                if (readBytes > 0) {
                    mFileOutputStream.write(mBuffer, 0, readBytes);
                } else {
                    return false;
                }
            }

            return stopRecord();

        } catch (IOException | RuntimeException e) {
            Log.e(TAG, "录音失败。", e);
            return false;
        } finally {
            if (mAudioRecord != null) {
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;
            }
        }

    }

    private boolean stopRecord() throws IOException {
        mAudioRecord.stop();
        mFileOutputStream.close();

        mEndRecordInMillis = System.currentTimeMillis();

        final int recordPeriodInSecond = (int) (mEndRecordInMillis - mBeginRecordInMillis) / 1000;

        if (recordPeriodInSecond >= 3) {
            UiThreadUtils.runInUIThread(new Runnable() {
                @Override
                public void run() {
                    mTvLog.setText(mTvLog.getText() + "\n录音时长：" + recordPeriodInSecond + "秒!");
                }
            });
        } else {
            mAudioFile.delete();
        }

        return true;
    }

    private boolean createAudioFile() {
        try {
            mAudioFile = RecordAudioUtils.createAudioFile(RecordAudioUtils.AUDIO_M4A);
        } catch (IOException e) {
            Log.e(TAG, "开始录音时，创建文件失败。", e);
            return false;
        }
        return true;
    }

    private void echoFail() {
        UiThreadUtils.showToast(RecordAudioByStreamActivity.this, "录音失败");
    }

    /**
     * running in background thread
     * @param audioFile
     */
    private void doPlayAudio(File audioFile) {
        mIsplaying = true;
        int streamType = AudioManager.STREAM_MUSIC;
        int sampleRateHz = 44100;
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int mode = AudioTrack.MODE_STREAM;
        int minBufferSize = AudioTrack.getMinBufferSize(sampleRateHz, channelConfig, audioFormat);

        AudioTrack audioTrack = new AudioTrack(streamType, sampleRateHz, channelConfig, audioFormat
                ,Math.max(minBufferSize, BUFFER_SIZE), mode);

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(audioFile);

            audioTrack.play() ;//开始

            int readBytes = 0;
            while ( (readBytes = fis.read(mBuffer)) > 0 ) {
                int ret = audioTrack.write(mBuffer, 0, readBytes);
                Log.i(TAG, "write bytes to audioTrack :" + readBytes);

                switch (ret) {
                    case AudioTrack.ERROR_INVALID_OPERATION:
                    case AudioTrack.ERROR_BAD_VALUE:
                    case AudioTrack.ERROR_DEAD_OBJECT:
                        echoPlayFail();
                        break;
                    default:
                        break;
                }
            }

        } catch (RuntimeException | IOException e) {
            Log.e(TAG, "播放录音失败。", e);
            echoPlayFail();
        } finally {
            mIsplaying = false;
            closeQuiety(fis);
            releaseAudioTrackQuiety(audioTrack);
        }
    }

    private void releaseAudioTrackQuiety(AudioTrack audioTrack) {
        if (audioTrack == null) {
            return;
        }

        audioTrack.stop();
        audioTrack.release();
    }

    private void closeQuiety(Closeable fis) {
        mIsplaying = false;
        
        if (fis == null) {
            return;
        }

        try {
            fis.close();
        } catch (IOException e) {
            Log.e(TAG, "关闭失败。", e);
        }
    }

    /**
     * running in background thread
     */
    private void echoPlayFail() {
        UiThreadUtils.showToast(RecordAudioByStreamActivity.this, "插放录音失败");
    }

}
