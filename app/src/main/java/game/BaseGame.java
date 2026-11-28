package game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import aircraft.AbstractTrackingEnemyAircraft;
import aircraft.HeroAircraft;
import application.ImageManager;
import application.Main;
import basic.AbstractFlyingObject;
import bullet.BaseBullet;
import enums.DifficultyEnums;

// SurfaceView 提供游戏的绘制表面，Callback 接收表面状态变化，Runnable 提供绘图线程的入口。
public abstract class BaseGame extends SurfaceView
        implements SurfaceHolder.Callback, Runnable {
    private static final long FRAME_DELAY_MS = 40;

    private HeroAircraft heroAircraft;
    protected Bitmap backGroundImage;
    // FILTER_BITMAP_FLAG 对缩放后的位图做插值，避免边缘过于生硬。
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    // 主线程保存当前绘图线程的引用，等旧线程退出后才允许创建下一条。
    private Thread worker;

    protected int enemyMaxNumber;
    protected int bossSpawnScore;
    protected int score;
    protected int difficultyFactor = 1;

    protected double enemySpawnCycle;
    protected double heroShootCycle;

    private boolean initialized;
    // Activity 恢复和 Surface 就绪是两个独立条件，主线程收到各自的回调后更新标记。
    private boolean resumed;
    private boolean surfaceReady;
    // 主线程写入停止信号，绘图线程下次检查 while 条件时能读到新值。
    private volatile boolean running;
    protected boolean allowBoss;

    protected DifficultyEnums difficulty;

    /** 创建游戏视图并注册绘图表面的生命周期回调。 */
    public BaseGame(Context context) {
        // 将当前 Activity 的资源和主题环境交给 Android 的视图构造器。
        super(context);
        // 注册当前 BaseGame，接收 Surface 创建、尺寸变化和即将销毁的回调。
        getHolder().addCallback(this);
    }

    /** 首次准备图片和本局状态，后续 Surface 重建时恢复绘制。 */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!initialized) {
            // 英雄构造器要读取图片高度，难度初始化要选择背景，因此先加载位图。
            ImageManager.load(getResources());
            heroAircraft = HeroAircraft.getInstance();
            initDifficultyParams();
            initialized = true;
        }
        // Surface 重建时继续使用已经初始化的英雄和背景。
        surfaceReady = true;
        startIfReady();
    }

    /** 接收系统报告的 Surface 格式与尺寸变化。 */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // render() 每帧都从 Canvas 读取实际宽高，尺寸变化后会重新计算缩放和留边。
    }

    /** 在 Surface 销毁回调结束前停止使用绘图缓冲。 */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceReady = false;
        // 回调返回前要等旧线程退出，保证它不再向即将销毁的 Surface 提交画面。
        stopAndJoin();
    }

    // Activity 首次进入或回到前台时调用。Surface 可能还在创建，交给 startIfReady() 判断。
    public void onHostResume() {
        resumed = true;
        startIfReady();
    }

    // 切后台时先停画，即使此时 Surface 还没有销毁。
    public void onHostPause() {
        resumed = false;
        stopAndJoin();
    }

    /** 前台状态和 Surface 同时就绪时启动一条游戏线程。 */
    private void startIfReady() {
        // 两个就绪条件都满足，而且目前没有绘图线程，才启动一个新线程。
        if (!resumed || !surfaceReady || worker != null) {
            return;
        }
        running = true;
        // this 实现了 Runnable，start() 会在新线程中执行下面的 run()。
        worker = new Thread(this, "AircraftGame");
        worker.start();
    }

    /** 发出停止信号并等待旧的工作线程彻底退出。 */
    private void stopAndJoin() {
        running = false;
        Thread old = worker;
        if (old == null) {
            return;
        }
        // interrupt() 可以唤醒正在 sleep() 的线程，join() 则等它真正结束。
        old.interrupt();
        boolean interrupted = false;
        while (old.isAlive()) {
            try {
                old.join();
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        worker = null;
        if (interrupted) {
            // 等待期间收到的中断，在确认旧线程停稳后还给当前线程。
            Thread.currentThread().interrupt();
        }
    }

    /** 工作线程反复绘制静态背景和英雄机。 */
    @Override
    public void run() {
        SurfaceHolder holder = getHolder();
        while (running) {
            // 锁定当前帧的绘制缓冲，系统可能暂时没有画布可供绘制。
            Canvas canvas = holder.lockCanvas();
            if (canvas != null) {
                try {
                    render(canvas);
                } finally {
                    // 只要取得了 Canvas，就要归还并提交这一帧。
                    holder.unlockCanvasAndPost(canvas);
                }
            }

            try {
                // 静态帧之间休眠一小段时间，避免循环空转占用 CPU。
                Thread.sleep(FRAME_DELAY_MS);
            } catch (InterruptedException e) {
                // stopAndJoin() 先写 running，再用 interrupt() 唤醒休眠中的线程。
                if (!running) {
                    break;
                }
            }
        }
    }

    /** 计算能容纳整个逻辑画面的最大等比缩放值。 */
    private float scale(int width, int height) {
        // 按更紧的一边等比缩放，让 512 × 768 的逻辑画面完整落在 Canvas 里。
        return Math.min(width / (float) Main.WINDOW_WIDTH, height / (float) Main.WINDOW_HEIGHT);
    }

    /** 在逻辑区内绘制一帧静态背景和英雄机。 */
    private void render(Canvas canvas) {
        // 先清整块画布，逻辑画面以外留下黑边。
        canvas.drawColor(Color.BLACK);
        float s = scale(canvas.getWidth(), canvas.getHeight());
        float offsetX = (canvas.getWidth() - Main.WINDOW_WIDTH * s) / 2;
        float offsetY = (canvas.getHeight() - Main.WINDOW_HEIGHT * s) / 2;

        int saved = canvas.save();
        try {
            // 先把原点移到游戏区左上角，再让后面的逻辑坐标统一按 s 缩放。
            canvas.translate(offsetX, offsetY);
            canvas.scale(s, s);
            canvas.clipRect(0, 0, Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT);

            // null 表示使用整张源图，目标矩形让背景铺满游戏区。
            RectF background = new RectF(0, 0, Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT);
            canvas.drawBitmap(backGroundImage, null, background, paint);
            drawObject(canvas, heroAircraft);
        } finally {
            // 恢复进入本帧前的 Canvas 状态，把缩放、平移和裁剪限制在这次绘制里。
            canvas.restoreToCount(saved);
        }
    }

    /** 在实体的中心绘制位图，子弹和追踪敌机按飞行方向旋转。 */
    private void drawObject(Canvas canvas, AbstractFlyingObject object) {
        Bitmap image = object.getImage();
        float halfWidth = image.getWidth() / 2f;
        float halfHeight = image.getHeight() / 2f;
        int saved = canvas.save();
        try {
            // 模型给的是图片中心。把原点移到中心，旋转时图片就围绕自身转动。
            canvas.translate(object.getLocationX(), object.getLocationY());
            if (object instanceof BaseBullet || object instanceof AbstractTrackingEnemyAircraft) {
                // 模型角度以向右为 0°、向下为 90°。沿用素材向下时无需旋转的约定。
                // Canvas.rotate() 围绕当前原点旋转，前面的 translate() 已将原点移到实体中心。
                canvas.rotate((float) object.getAngle() - 90f);
            }
            // 用位图半宽、半高定义各边，RectF 的中心就与当前原点重合。
            canvas.drawBitmap(image, null,
                    new RectF(-halfWidth, -halfHeight, halfWidth, halfHeight), paint);
        } finally {
            // 恢复到游戏区坐标，下一架飞机的绘制从自己的中心开始。
            canvas.restoreToCount(saved);
        }
    }

    /** 各难度在这里设置背景、刷怪上限和射击间隔。 */
    protected abstract void initDifficultyParams();

    /** 每个逻辑步按当前得分调整难度，具体规则由各难度类提供。 */
    protected void updateDifficultyProgression() {}
}
