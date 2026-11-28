package game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import aircraft.AbstractEnemyAircraft;
import aircraft.AbstractTrackingEnemyAircraft;
import aircraft.BossEnemy;
import aircraft.HeroAircraft;
import application.ImageManager;
import application.Main;
import basic.AbstractFlyingObject;
import bullet.BaseBullet;
import enemySpawnStrategy.BossSpawn;
import enemySpawnStrategy.NormalSpawn;
import enums.DifficultyEnums;
import props.AbstractProps;
import props.BombProps;

import java.util.ArrayList;
import java.util.List;

// SurfaceView 提供游戏的绘制表面，Callback 接收表面状态变化，Runnable 提供绘图线程的入口。
public abstract class BaseGame extends SurfaceView
        implements SurfaceHolder.Callback, Runnable {
    private HeroAircraft heroAircraft;
    protected Bitmap backGroundImage;
    // FILTER_BITMAP_FLAG 对缩放后的位图做插值，ANTI_ALIAS_FLAG 让文字边缘更平滑。
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    // 主线程保存当前绘图线程的引用，等旧线程退出后才允许创建下一条。
    private Thread worker;

    // PointF 保存一对浮点坐标。触摸回调替换整个对象，游戏线程读到后再移动英雄机。
    private volatile PointF touchPoint;
    // 游戏状态只由工作线程推进。首次初始化发生在工作线程启动之前。
    private final List<AbstractEnemyAircraft> enemyAircrafts = new ArrayList<>();
    private final List<AbstractEnemyAircraft> bosses = new ArrayList<>();
    private final List<BaseBullet> heroBullets = new ArrayList<>();
    private final List<BaseBullet> enemyBullets = new ArrayList<>();
    private final List<AbstractProps> props = new ArrayList<>();
    private NormalSpawn normalSpawn;

    // 本局的计数和分数由游戏线程更新。
    private int spawnCounter;
    private int shootCounter;
    private int bossCount;
    private int backgroundOffset;
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
    private boolean gameOver;
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
            heroAircraft.resetForGame();
            initDifficultyParams();
            normalSpawn = new NormalSpawn();
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
        touchPoint = null;
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

    /** 每轮更新游戏状态、画一帧，再休眠到下一轮。 */
    @Override
    public void run() {
        SurfaceHolder holder = getHolder();
        while (running) {
            if (!gameOver) {
                tick();
            }

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
                // 每画一帧让出 CPU 约 40 ms，退出时 interrupt() 会唤醒这里。
                Thread.sleep(Main.TICK_DURATION_MS);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    /** 将手指坐标从 Android View 的像素空间换算到游戏的逻辑坐标。 */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // MotionEvent 将手指按下、拖动、松开分别作为不同的事件交给这个视图。
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            float s = scale(getWidth(), getHeight());
            if (s > 0) {
                // 逆用 render() 的变换，先减去居中的留边，再除以缩放比例。
                float offsetX = (getWidth() - Main.WINDOW_WIDTH * s) / 2;
                float offsetY = (getHeight() - Main.WINDOW_HEIGHT * s) / 2;
                touchPoint = new PointF((event.getX() - offsetX) / s, (event.getY() - offsetY) / s);
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            touchPoint = null;
        }
        // 消费手势，Android 才会继续分发后面的移动和松手事件。
        return true;
    }

    /** 推进一格游戏时间，依次处理输入、效果、生成、移动、碰撞和结算。 */
    private void tick() {
        PointF input = touchPoint;
        if (input != null) {
            // 模型坐标是机身中心，夹到边界时要预留半个机身的空间。
            float halfWidth = heroAircraft.getWidth() / 2f;
            float halfHeight = heroAircraft.getHeight() / 2f;
            float x = Math.max(halfWidth, Math.min(Main.WINDOW_WIDTH - halfWidth, input.x));
            float y = Math.max(halfHeight, Math.min(Main.WINDOW_HEIGHT - halfHeight, input.y));
            heroAircraft.setLocation(x, y);
        }

        heroAircraft.tickEffects();
        updateDifficultyProgression();
        for (BaseBullet bullet : enemyBullets) {
            bullet.tickEffects();
        }

        if (++spawnCounter >= enemySpawnCycle && enemyAircrafts.size() + bosses.size() < enemyMaxNumber) {
            generateEnemy();
            spawnCounter = 0;
        }
        if (++shootCounter >= heroShootCycle) {
            heroBullets.addAll(heroAircraft.shoot());
            shootCounter = 0;
        }

        List<AbstractEnemyAircraft> enemies = new ArrayList<>(enemyAircrafts);
        enemies.addAll(bosses);
        for (AbstractEnemyAircraft enemy : enemies) {
            enemy.tickEffects();
            enemyBullets.addAll(enemy.shootByTick());
        }

        heroBullets.forEach(BaseBullet::forward);
        enemyBullets.forEach(BaseBullet::forward);
        enemies.forEach(AbstractEnemyAircraft::forward);
        props.forEach(AbstractProps::forward);
        crashCheckAction(enemies);

        // 碰撞期间只设置失效标记，在遍历结束后一起清理实体。
        enemyAircrafts.removeIf(AbstractFlyingObject::notValid);
        bosses.removeIf(AbstractFlyingObject::notValid);
        heroBullets.removeIf(AbstractFlyingObject::notValid);
        enemyBullets.removeIf(AbstractFlyingObject::notValid);
        props.removeIf(AbstractFlyingObject::notValid);

        backgroundOffset = (backgroundOffset + 2) % Main.WINDOW_HEIGHT;
        if (heroAircraft.getHp() <= 0) {
            // 本局结束后继续显示结算画面，停止更新游戏对象。
            gameOver = true;
        }
    }

    /** 按难度和分数选择生成普通敌机或 Boss，再放入本局列表。 */
    private void generateEnemy() {
        boolean bossDue = allowBoss && bossSpawnScore > 0 && bosses.isEmpty()
                && score / bossSpawnScore > bossCount;
        List<AbstractEnemyAircraft> spawned = (bossDue ? new BossSpawn(difficulty) : normalSpawn)
                .spawnEnemy(difficultyFactor);
        for (AbstractEnemyAircraft enemy : spawned) {
            if (enemy instanceof BossEnemy) {
                bosses.add(enemy);
                bossCount++;
            } else if (enemy != null) {
                enemyAircrafts.add(enemy);
            }
        }
    }

    /** 对这一逻辑步中的实体完成四轮碰撞，处理扣血、得分和拾取道具。 */
    private void crashCheckAction(List<AbstractEnemyAircraft> enemies) {
        // 1. 敌弹撞英雄。第一发命中后开启短暂无敌，本轮剩下的敌弹也会读到它。
        for (BaseBullet bullet : enemyBullets) {
            if (!bullet.notValid() && !heroAircraft.notValid()
                    && !heroAircraft.isInvincible() && heroAircraft.crash(bullet)) {
                bullet.vanish();
                heroAircraft.decreaseHp(bullet.getPower());
                heroAircraft.setInvincible(true);
                heroAircraft.setExpectedInvincibleTime(40);
            }
        }

        // 2. 英雄弹撞敌机。命中后立即失效并退出内层循环，一发子弹只结算一次。
        for (BaseBullet bullet : heroBullets) {
            if (bullet.notValid()) {
                continue;
            }
            for (AbstractEnemyAircraft enemy : enemies) {
                if (enemy.notValid() || !enemy.crash(bullet)) {
                    continue;
                }
                bullet.vanish();
                enemy.decreaseHp(bullet.getPower());
                if (enemy.notValid()) {
                    score += enemy.getScore();
                    props.addAll(enemy.getPropsIfVanish());
                }
                break;
            }
        }

        // 3. 撞机独立检查，场上没有英雄弹时也能触发。
        for (AbstractEnemyAircraft enemy : enemies) {
            if (!enemy.notValid() && !heroAircraft.notValid()
                    && !heroAircraft.isInvincible() && enemy.crash(heroAircraft)) {
                enemy.vanish();
                heroAircraft.decreaseHp(heroAircraft.getHp());
                break;
            }
        }

        if (heroAircraft.notValid()) {
            return;
        }

        // 4. 拾取道具。炸弹只统计从这次爆炸前存活变为死亡的敌机。
        for (AbstractProps prop : props) {
            if (prop.notValid() || !prop.crash(heroAircraft)) {
                continue;
            }
            List<AbstractEnemyAircraft> alive = new ArrayList<>();
            for (AbstractEnemyAircraft enemy : enemies) {
                if (!enemy.notValid()) {
                    alive.add(enemy);
                }
            }
            prop.functionOnEnemy(alive, enemyBullets);
            prop.functionOnHero();
            prop.vanish();
            if (prop instanceof BombProps) {
                for (AbstractEnemyAircraft enemy : alive) {
                    if (enemy.notValid()) {
                        score += enemy.getScore();
                    }
                }
            }
        }
    }

    /** 计算能容纳整个逻辑画面的最大等比缩放值。 */
    private float scale(int width, int height) {
        // 按更紧的一边等比缩放，让 512 × 768 的逻辑画面完整落在 Canvas 里。
        return Math.min(width / (float) Main.WINDOW_WIDTH, height / (float) Main.WINDOW_HEIGHT);
    }

    /** 绘制一帧画面，包括滚动背景、实体、分数和生命值。 */
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

            // drawBitmap() 的 null 源矩形表示整张图。两份背景上下紧贴，向下滚动时始终铺满逻辑区。
            RectF background = new RectF(0, backgroundOffset - Main.WINDOW_HEIGHT,
                    Main.WINDOW_WIDTH, backgroundOffset);
            canvas.drawBitmap(backGroundImage, null, background, paint);
            background.offset(0, Main.WINDOW_HEIGHT);
            canvas.drawBitmap(backGroundImage, null, background, paint);

            drawObjects(canvas, enemyBullets);
            drawObjects(canvas, heroBullets);
            drawObjects(canvas, enemyAircrafts);
            drawObjects(canvas, bosses);
            drawObjects(canvas, props);
            if (!heroAircraft.notValid()) {
                drawObject(canvas, heroAircraft);
            }

            paint.setColor(Color.WHITE);
            paint.setTextSize(22);
            // drawText() 的 y 坐标是文字基线，文字高度会随游戏区一起缩放。
            canvas.drawText("SCORE: " + score, 10, 28, paint);
            canvas.drawText("LIFE: " + heroAircraft.getHp() + " / " + heroAircraft.getMaxHp(), 10, 56, paint);
            if (gameOver) {
                String result = "GAME OVER";
                paint.setColor(Color.RED);
                paint.setTextSize(38);
                float resultX = (Main.WINDOW_WIDTH - paint.measureText(result)) / 2;
                canvas.drawText(result, resultX, Main.WINDOW_HEIGHT / 2f, paint);
            }
        } finally {
            // 恢复进入本帧前的 Canvas 状态，把缩放、平移和裁剪限制在这次绘制里。
            canvas.restoreToCount(saved);
        }
    }

    /** 遍历一组对象并画出其中仍然有效的位图。 */
    private void drawObjects(Canvas canvas, List<? extends AbstractFlyingObject> objects) {
        for (AbstractFlyingObject object : objects) {
            if (!object.notValid()) {
                drawObject(canvas, object);
            }
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
