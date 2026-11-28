package main;

import android.app.Activity;
import android.os.Bundle;
import game.BaseGame;
import game.EasyGame;

public class MainActivity extends Activity {
    private BaseGame game;

    /** 创建并挂载简单模式的游戏视图。 */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Activity 本身就是 Context，把当前页面的环境传给简单模式的游戏视图。
        game = new EasyGame(this);
        setContentView(game);
    }

    /** 将 Activity 恢复事件转交给游戏视图。 */
    @Override
    protected void onResume() {
        super.onResume();
        // 页面已经恢复。BaseGame 会再检查 Surface 是否可以绘制。
        game.onHostResume();
    }

    /** 在 Activity 暂停时通知游戏视图停止更新和绘制。 */
    @Override
    protected void onPause() {
        // 页面暂停时停止绘图，避免应用切到后台后仍重复画帧。
        game.onHostPause();
        super.onPause();
    }
}
