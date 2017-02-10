package codepath.com.cn.imaudio.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.IntDef;
import android.widget.Toast;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * 其它线程向主线程提交UI更新任务或简易文本提示消息
 *
 * @author LiXiaoPing(17773406760@189.cn)
 *
 *
 * Created by  on 2017/2/10.
 */

public final class UiThreadUtils {

    @IntDef({Toast.LENGTH_SHORT, Toast.LENGTH_LONG})
    @Retention(RetentionPolicy.SOURCE)
    @interface Duration {}

    /**
     * 显示简易文本提示消息
     * @param context
     * @param text 文本提示消息
     * @param duration 显示时长 {@link android.widget.Toast}.LENGTH_SHORT,
     *                         {@link android.widget.Toast}.LENGTH_LONG
     *
     *
     */
    public static void showToast(final Context context, final CharSequence text,
                                 @Duration final int duration) {
        UiThreadHandler.INSTANCE.showToast(context, text, duration);
    }

    /**
     * 默认时长显示简易文本提示消息
     * @param context
     * @param text 文本提示消息
     */
    public static void showToast(final Context context, final CharSequence text) {
        showToast(context, text, Toast.LENGTH_SHORT);
    }

    /**
     * 向主线程提交UI更新任务
     * @param task
     */
    public static void runInUIThread(Runnable task) {
        UiThreadHandler.INSTANCE.runInUIThread(task);
    }


    /**
     * 实现单例模式
     */
    private enum UiThreadHandler {
        INSTANCE;

        private Handler mHandler;

        UiThreadHandler() {
            mHandler = new Handler(Looper.getMainLooper());
        }

        void showToast(final Context context, final CharSequence text, final int duration) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, text, duration).show();
                }
            });
        }

        void runInUIThread(Runnable task) {
            mHandler.post(task);
        }

    }
}





