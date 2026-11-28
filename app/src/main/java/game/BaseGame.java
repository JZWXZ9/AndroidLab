package game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
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

/**
 * 游戏主视图基类（对应原桌面版的 AbstractGame，整体结构对照课件 SurfaceViewDemo）
 *
 * 继承/实现的三个 Android 核心组件：
 * 1. SurfaceView：自带独立绘图表面（Surface），允许在子线程高频画图，不会卡住主界面。
 * 2. SurfaceHolder.Callback：监听 Surface 的创建、尺寸变化和销毁，用来控制绘图线程启停。
 * 3. Runnable：作为独立绘图线程的入口，在 run() 里循环执行“更新状态 → 绘制一帧 → 休眠”。
 */
public abstract class BaseGame extends SurfaceView
        implements SurfaceHolder.Callback, Runnable {

    private HeroAircraft heroAircraft;
    protected Bitmap backGroundImage;

    // Paint 是 Android 的画笔，用来设置颜色、字号、字体和位图缩放滤波。
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    // SurfaceHolder 是 Surface 的管理器，用来注册回调、借出画布（lockCanvas）和提交画布（unlockCanvasAndPost）。
    private final SurfaceHolder surfaceHolder;

    // 控制游戏循环 while (running) 是否继续执行的标志位（对应课件中的 mbLoop）。
    private boolean running = false;
    // 标记本局是否已经做过首次图片加载与状态初始化，切后台再回来重建 Surface 时不重置分数。
    private boolean initialized = false;
    private boolean gameOver = false;

    // 本局中的各类飞行对象列表。
    private final List<AbstractEnemyAircraft> enemyAircrafts = new ArrayList<>();
    private final List<AbstractEnemyAircraft> bosses = new ArrayList<>();
    private final List<BaseBullet> heroBullets = new ArrayList<>();
    private final List<BaseBullet> enemyBullets = new ArrayList<>();
    private final List<AbstractProps> props = new ArrayList<>();
    private NormalSpawn normalSpawn;

    // 刷怪、射击、Boss 和滚动背景的计数器。
    private int spawnCounter;
    private int shootCounter;
    private int bossCount;
    private int backgroundOffset;

    // 以下 protected 参数由四个难度子类（EasyGame / NormalGame 等）在 initDifficultyParams() 中赋值。
    protected int enemyMaxNumber;
    protected int bossSpawnScore;
    protected int score;
    protected int difficultyFactor = 1;
    protected double enemySpawnCycle;
    protected double heroShootCycle;
    protected boolean allowBoss;
    protected DifficultyEnums difficulty;

    /**
     * 构造游戏视图：加载像素字体，拿到 SurfaceHolder 并注册本类为 Surface 回调监听者。
     */
    public BaseGame(Context context) {
        super(context);
        // 通过 AssetManager（context.getAssets()）读取 app/src/main/assets/fonts/ 下的像素字体。
        paint.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/PixelOperator.ttf"));
        // 获取 SurfaceHolder 并注册回调，随后系统会在 Surface 创建/变化/销毁时自动调用下面三个方法。
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        // 允许当前视图获取焦点，正常接收屏幕触摸事件。
        setFocusable(true);
    }

    /**
     * 【SurfaceHolder.Callback 回调 1】底层 Surface 绘图表面创建好后由系统自动调用。
     * 首次进入先加载图片再初始化英雄机与难度，然后启动游戏绘图线程。
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!initialized) {
            // 必须先加载图片再创建英雄机，因为 HeroAircraft 构造器会立刻读取 HERO_IMAGE.getHeight()。
            ImageManager.load(getResources());
            heroAircraft = HeroAircraft.getInstance();
            heroAircraft.resetForGame();
            initDifficultyParams();
            normalSpawn = new NormalSpawn();
            initialized = true;
        }
        running = true;
        new Thread(this).start();
    }

    /**
     * 【SurfaceHolder.Callback 回调 2】Surface 尺寸或格式变化时由系统调用。
     * 本类在 render() 里每帧直接读取 Canvas 的当前宽高算缩放，这里不需要额外缓存。
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    /**
     * 【SurfaceHolder.Callback 回调 3】Surface 即将销毁前由系统调用（例如按 Home 切后台或退出页面）。
     * 把循环标志设为 false，让游戏线程结束循环。
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        running = false;
    }

    /**
     * 游戏主循环（运行在子线程中，结构对照课件 SurfaceViewDemo）：
     * 每一轮同步锁住 surfaceHolder，推进一帧游戏逻辑并绘制上屏，再休眠约 40 ms。
     */
    @Override
    public void run() {
        while (running) {
            synchronized (surfaceHolder) {
                if (!gameOver) {
                    tick();
                }
                draw();
            }
            try {
                // 每帧休眠约 40 ms（约 25 帧/秒），与原桌面版节奏一致。
                Thread.sleep(Main.TICK_DURATION_MS);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 向 SurfaceHolder 借出当前帧的 Canvas 画布，画完后提交上屏。
     */
    private void draw() {
        // lockCanvas() 从系统借出当前可绘制的 Canvas；Surface 不可用时会返回 null。
        Canvas canvas = surfaceHolder.lockCanvas();
        if (canvas == null) {
            return;
        }
        render(canvas);
        // unlockCanvasAndPost() 把画好的画面归还给系统并显示到屏幕上。
        surfaceHolder.unlockCanvasAndPost(canvas);
    }

    /**
     * 【View 触摸回调】替代桌面版的 HeroController 鼠标监听。
     * 手指按下或拖动时，把手机屏幕像素坐标换算回 512 × 768 游戏逻辑坐标，直接移动英雄机。
     *
     * @param event Android 封装的触摸事件（getAction() 区分按下/拖动，getX()/getY() 取手指位置）
     * @return 返回 true 表示消费本次手势，系统才会继续把后续的 ACTION_MOVE 传进来
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if ((action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE)
                && heroAircraft != null && !gameOver) {
            float s = scale(getWidth(), getHeight());
            if (s > 0) {
                // 逆用 render() 中的居中平移和缩放：先减去黑边偏移，再除以缩放比例。
                float offsetX = (getWidth() - Main.WINDOW_WIDTH * s) / 2;
                float offsetY = (getHeight() - Main.WINDOW_HEIGHT * s) / 2;
                float x = (event.getX() - offsetX) / s;
                float y = (event.getY() - offsetY) / s;

                // 英雄机坐标是图片中心，限制在边界内时四周各留半个机身宽高，避免半边出界。
                float halfWidth = heroAircraft.getWidth() / 2f;
                float halfHeight = heroAircraft.getHeight() / 2f;
                x = Math.max(halfWidth, Math.min(Main.WINDOW_WIDTH - halfWidth, x));
                y = Math.max(halfHeight, Math.min(Main.WINDOW_HEIGHT - halfHeight, y));
                heroAircraft.setLocation(x, y);
            }
        }
        return true;
    }

    /**
     * 推进一格游戏逻辑（每 40 ms 执行一次）：
     * 依次完成 效果倒计时 → 刷怪与射击 → 移动 → 碰撞结算 → 清理无效对象 → 背景滚动与死亡判断。
     */
    private void tick() {
        // 1. 推进英雄机无敌/火力倒计时、难度递进，以及敌方子弹的冰冻倒计时。
        heroAircraft.tickEffects();
        updateDifficultyProgression();
        for (BaseBullet bullet : enemyBullets) {
            bullet.tickEffects();
        }

        // 2. 按周期生成敌机，按周期触发英雄机自动射击。
        if (++spawnCounter >= enemySpawnCycle && enemyAircrafts.size() + bosses.size() < enemyMaxNumber) {
            generateEnemy();
            spawnCounter = 0;
        }
        if (++shootCounter >= heroShootCycle) {
            heroBullets.addAll(heroAircraft.shoot());
            shootCounter = 0;
        }

        // 3. 合并普通敌机与 Boss，统一推进冰冻计时和敌机射击。
        List<AbstractEnemyAircraft> enemies = new ArrayList<>(enemyAircrafts);
        enemies.addAll(bosses);
        for (AbstractEnemyAircraft enemy : enemies) {
            enemy.tickEffects();
            enemyBullets.addAll(enemy.shootByTick());
        }

        // 4. 所有飞行对象移动一步，随后做碰撞检测。
        heroBullets.forEach(BaseBullet::forward);
        enemyBullets.forEach(BaseBullet::forward);
        enemies.forEach(AbstractEnemyAircraft::forward);
        props.forEach(AbstractProps::forward);
        crashCheckAction(enemies);

        // 5. 统一清理已标记无效的对象，避免在遍历过程中直接删除元素报错。
        enemyAircrafts.removeIf(AbstractFlyingObject::notValid);
        bosses.removeIf(AbstractFlyingObject::notValid);
        heroBullets.removeIf(AbstractFlyingObject::notValid);
        enemyBullets.removeIf(AbstractFlyingObject::notValid);
        props.removeIf(AbstractFlyingObject::notValid);

        // 6. 推进背景向下滚动偏移，并判断英雄机是否血量归零。
        backgroundOffset = (backgroundOffset + 2) % Main.WINDOW_HEIGHT;
        if (heroAircraft.getHp() <= 0) {
            gameOver = true;
        }
    }

    /**
     * 按当前难度和得分决定生成 Boss 还是普通敌机，并加入对应列表。
     */
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

    /**
     * 执行本帧的四轮碰撞判定：敌弹打英雄、英雄弹打敌机、敌机撞英雄、英雄拾取道具。
     */
    private void crashCheckAction(List<AbstractEnemyAircraft> enemies) {
        // 1. 敌方子弹命中英雄机（命中后开启 40 tick 无敌保护）。
        for (BaseBullet bullet : enemyBullets) {
            if (!bullet.notValid() && !heroAircraft.notValid()
                    && !heroAircraft.isInvincible() && heroAircraft.crash(bullet)) {
                bullet.vanish();
                heroAircraft.decreaseHp(bullet.getPower());
                heroAircraft.setInvincible(true);
                heroAircraft.setExpectedInvincibleTime(40);
            }
        }

        // 2. 英雄子弹命中敌机（命中即 vanish 并 break，防止一发子弹穿透两架重叠敌机）。
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

        // 3. 敌机撞击英雄机（单独遍历敌机，不嵌套在英雄子弹循环里，保证无子弹时也能撞机）。
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

        // 4. 英雄机拾取道具（只给本次炸弹新炸死的敌机加分，避免和同帧子弹击杀重复计分）。
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

    /**
     * 计算能把 512 × 768 逻辑画面完整塞进当前屏幕的最大等比缩放系数。
     */
    private float scale(int width, int height) {
        return Math.min(width / (float) Main.WINDOW_WIDTH, height / (float) Main.WINDOW_HEIGHT);
    }

    /**
     * 绘制整帧画面：清黑边 → 建立 512 × 768 逻辑坐标系 → 画双图滚动背景 → 画实体 → 画分数与血量。
     *
     * Canvas 坐标系变换说明：
     * - canvas.save()     保存当前画布坐标系（压栈）
     * - canvas.translate  把画布原点 (0, 0) 移到居中游戏区左上角
     * - canvas.scale      把后续所有坐标等比放大 s 倍，后面直接按 512 × 768 画即可
     * - canvas.clipRect   裁剪逻辑区以外的部分，避免边缘内容画到黑边上
     * - canvas.restore()  恢复进入本帧前的画布状态（弹栈）
     */
    private void render(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        float s = scale(canvas.getWidth(), canvas.getHeight());
        float offsetX = (canvas.getWidth() - Main.WINDOW_WIDTH * s) / 2;
        float offsetY = (canvas.getHeight() - Main.WINDOW_HEIGHT * s) / 2;

        canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.scale(s, s);
        canvas.clipRect(0, 0, Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT);

        // 两张背景上下紧贴绘制，实现无缝向下滚动：
        // drawBitmap 第 2 参数传 null 表示画整张原图；RectF.offset(0, 768) 把绘制框向下平移一屏高度。
        RectF background = new RectF(0, backgroundOffset - Main.WINDOW_HEIGHT,
                Main.WINDOW_WIDTH, backgroundOffset);
        canvas.drawBitmap(backGroundImage, null, background, paint);
        background.offset(0, Main.WINDOW_HEIGHT);
        canvas.drawBitmap(backGroundImage, null, background, paint);

        // 按层次依次绘制子弹、敌机、Boss、道具和英雄机。
        drawObjects(canvas, enemyBullets);
        drawObjects(canvas, heroBullets);
        drawObjects(canvas, enemyAircrafts);
        drawObjects(canvas, bosses);
        drawObjects(canvas, props);
        if (!heroAircraft.notValid()) {
            drawObject(canvas, heroAircraft);
        }

        // 绘制左上角分数与生命值（drawText 的 y 是文字底部基线位置）。
        paint.setColor(Color.WHITE);
        paint.setTextSize(24);
        canvas.drawText("SCORE: " + score, 10, 28, paint);
        canvas.drawText("LIFE: " + heroAircraft.getHp() + " / " + heroAircraft.getMaxHp(), 10, 56, paint);
        if (gameOver) {
            String result = "GAME OVER";
            paint.setColor(Color.RED);
            paint.setTextSize(40);
            // paint.measureText() 计算文字实际宽度，用来算出水平居中的 x 坐标。
            float resultX = (Main.WINDOW_WIDTH - paint.measureText(result)) / 2;
            canvas.drawText(result, resultX, Main.WINDOW_HEIGHT / 2f, paint);
        }

        canvas.restore();
    }

    /**
     * 遍历一组飞行对象，把其中仍然有效的对象逐个画到画布上。
     */
    private void drawObjects(Canvas canvas, List<? extends AbstractFlyingObject> objects) {
        for (AbstractFlyingObject object : objects) {
            if (!object.notValid()) {
                drawObject(canvas, object);
            }
        }
    }

    /**
     * 在单个实体的中心坐标处绘制位图；子弹和追踪敌机额外按速度方向旋转贴图。
     *
     * 旋转绘制四步走（为什么必须先 translate 再 rotate）：
     * 1. canvas.save()：保存当前 512 × 768 坐标系，避免这次旋转影响下一个对象。
     * 2. canvas.translate(cx, cy)：把画布原点移到实体中心，让图片绕自身中心转而不是绕屏幕左上角转。
     * 3. canvas.rotate(angle - 90f)：绕当前原点顺时针旋转。atan2 算出的 0° 朝右、90° 朝下，
     *    素材默认朝下，所以减 90° 后向下飞时角度为 0°（不转）。
     * 4. 以 (-halfW, -halfH, halfW, halfH) 为目标矩形画图（中心正好落在原点），再 canvas.restore() 还原。
     */
    private void drawObject(Canvas canvas, AbstractFlyingObject object) {
        Bitmap image = object.getImage();
        float halfWidth = image.getWidth() / 2f;
        float halfHeight = image.getHeight() / 2f;

        canvas.save();
        canvas.translate(object.getLocationX(), object.getLocationY());
        if (object instanceof BaseBullet || object instanceof AbstractTrackingEnemyAircraft) {
            canvas.rotate((float) object.getAngle() - 90f);
        }
        canvas.drawBitmap(image, null,
                new RectF(-halfWidth, -halfHeight, halfWidth, halfHeight), paint);
        canvas.restore();
    }

    /** 各难度子类在这里初始化背景图、敌机上限、刷怪周期、射击周期和 Boss 规则。 */
    protected abstract void initDifficultyParams();

    /** 每帧按当前得分调整难度参数，默认不做递进（由 NormalGame / HardGame 按需重写）。 */
    protected void updateDifficultyProgression() {
    }
}
