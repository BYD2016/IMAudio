package codepath.com.cn.imaudio;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import codepath.com.cn.imaudio.utils.RecordAudioUtils;
import codepath.com.cn.imaudio.utils.UiThreadUtils;

public class RecordAudioByFileActivity extends AppCompatActivity {

    private static final String TAG = RecordAudioByFileActivity.class.getSimpleName();

    @BindView(R.id.tvLog)
    TextView mTvLog;
    @BindView(R.id.tvPressToSay)
    Button mTvPressToSay;
    @BindView(R.id.btnPlayAudio)
    Button mBtnPlayAudio;

    private ExecutorService mExecutorService;
    private MediaRecorder mMediaRecorder;
    private File mAudioFile;

    private long mBeginRecordInMillis, mEndRecordInMillis;


    // 播放状态
    private volatile boolean mIsplaying;
    private MediaPlayer mMediaPlayer;

    public void setIsplaying(boolean isplaying) {
        mIsplaying = isplaying;

        mBtnPlayAudio.setText(
                mIsplaying ? R.string.record_audio_stop_playing : R.string.record_audio_playing);

        if (mIsplaying) {

            if (mAudioFile != null) {
                mExecutorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        doPlayAudio(mAudioFile);
                    }
                });
            } else {
                Toast.makeText(RecordAudioByFileActivity.this, "请先录音...",
                        Toast.LENGTH_SHORT).show();
            }


        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_audio_by_file);
        ButterKnife.bind(this);

        // 录音JNI函数不具备线程安全性，所以要用单线程
        mExecutorService = Executors.newSingleThreadExecutor();

        initUIControlerEventHandlers();
    }

    @Override
    protected void onDestroy() {
        mExecutorService.shutdownNow();
        releaseRecorder();
        stopPlay();
        super.onDestroy();
    }


    @OnClick(R.id.btnPlayAudio)
    void playAudioHandler() {
        setIsplaying(!mIsplaying);
    }

    private void initUIControlerEventHandlers() {
        mTvPressToSay.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startRecordAudio();
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        stopRecordAudio();
                        break;

                    default:
                        break;

                }

                return true;
            }
        });
    }

    /**
     * 开始录音
     */
    private void startRecordAudio() {
        mTvPressToSay.setText(R.string.record_audio_speaking);

        mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                releaseRecorder();

                if (!doStartRecordAudio()) {
                    echoFail();
                }
            }
        });

    }

    /**
     * 停止录音
     */
    private void stopRecordAudio() {
        mTvPressToSay.setText(R.string.record_audio_press_to_say);

        mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                if (!doStopRecordAudio()) {
                    echoFail();
                }

                releaseRecorder();
            }
        });

    }

    private boolean doStartRecordAudio() {
        mMediaRecorder = new MediaRecorder();

        // 配置
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setAudioSamplingRate(44100); //44.1kHz
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setAudioEncodingBitRate(96000); //96kbps

        // 创建录音文件
        if (!createAudioFile()) return false;

        mMediaRecorder.setOutputFile(mAudioFile.getAbsolutePath());

        try {
            mMediaRecorder.prepare();
            mMediaRecorder.start();
            mBeginRecordInMillis = System.currentTimeMillis();
        } catch (IOException | IllegalStateException e) {
            Log.e(TAG, "准备录音或启动录音时失败。", e);
            return false;
        }

        return true;
    }

    private boolean createAudioFile() {
        try {
            mAudioFile = RecordAudioUtils.createAudioFile(RecordAudioUtils.AUDIO_PCM);
        } catch (IOException e) {
            Log.e(TAG, "开始录音时，创建文件失败。", e);
            return false;
        }
        return true;
    }

    private boolean doStopRecordAudio() {

        try {
            mMediaRecorder.stop();
            mEndRecordInMillis = System.currentTimeMillis();

            final int recordPeriodInSecond = (int) ((mEndRecordInMillis - mBeginRecordInMillis) / 1000);

            // 只接受超过3秒的录音
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

        } catch (IllegalStateException e) {
            Log.e(TAG, "停止录音时失败。", e);
            return false;
        }

        return true;
    }

    /**
     * 反馈错误给用户
     */
    private void echoFail() {
        mAudioFile = null;
        UiThreadUtils.showToast(RecordAudioByFileActivity.this, "录音失败");
    }

    private void releaseRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }

    /**
     * running in background thread
     */
    private void doPlayAudio(File audioFile) {

        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setDataSource(audioFile.getAbsolutePath());
            mMediaPlayer.setVolume(1.0f, 1.0f);
            mMediaPlayer.setLooping(false);

            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                  stopPlay();
                }
            });

            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    echoPayFail();
                    stopPlay();
                    return true;
                }
            });

            mMediaPlayer.prepare();
            mMediaPlayer.start();

        } catch (RuntimeException | IOException e) {
            Log.e(TAG, "播放失败.", e);
            echoPayFail();
            stopPlay();
        }

    }

    /**
     * running in background thread
     */
    private void echoPayFail() {
        UiThreadUtils.showToast(RecordAudioByFileActivity.this, "插放失败");
    }

    private void stopPlay() {
        setIsplaying(false);

        if (mMediaPlayer != null) {
            mMediaPlayer.setOnErrorListener(null);
            mMediaPlayer.setOnCompletionListener(null);
            mMediaPlayer.stop();
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

}
