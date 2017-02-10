package codepath.com.cn.imaudio.utils;

import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import codepath.com.cn.imaudio.BuildConfig;


/**
 * 在SD卡上指定目录创建指定类型的音频文件
 *
 * @author LiXiaoPing(17773406760@189.cn)
 *
 *
 * Created by  on 2017/2/10.
 */

public final class RecordAudioUtils {

    private static final String TAG = RecordAudioUtils.class.getSimpleName();

    public static final String AUDIO_M4A = ".m4a";
    public static final String AUDIO_PCM = ".pcm";

    /** @hide */
    @StringDef({AUDIO_M4A, AUDIO_PCM})
    @Retention(RetentionPolicy.SOURCE)
    @interface AudioFileExtType {}

    @Nullable
    public static File createAudioFile(@AudioFileExtType String fileExtType) throws IOException {
        File audioFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/imooc/audio/" + obtainFileName() + fileExtType);

        File parentPath = audioFile.getParentFile();
        if (!parentPath.exists()) {
            parentPath.mkdirs();
        }

        audioFile.createNewFile();

        return audioFile;
    }

    private static String obtainFileName() {
        return BuildConfig.DEBUG ? "demo" : String.valueOf(System.currentTimeMillis());

    }
}
