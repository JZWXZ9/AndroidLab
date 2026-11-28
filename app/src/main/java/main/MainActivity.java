package main;

import android.app.Activity;
import android.os.Bundle;
import game.BaseGame;
import game.EasyGame;

/**
 * Android 启动入口页面（Activity 相当于一个屏幕界面）。
 * 在 Lab 2 中直接创建简单模式的游戏视图，并设置为当前页面的显示内容。
 */
public class MainActivity extends Activity {

    /**
     * Activity 首次创建时由系统调用：创建 EasyGame 视图并全屏挂载到界面上。
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Activity 本身就是 Context，把当前页面环境传给游戏视图用来读取资源和字体。
        BaseGame game = new EasyGame(this);
        // setContentView() 把游戏视图显示为整个 Activity 的内容。
        setContentView(game);
    }
}
