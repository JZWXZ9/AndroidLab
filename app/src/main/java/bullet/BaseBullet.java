package bullet;

import application.Main;
import basic.AbstractFlyingObject;
import observer.BombObserver;
import observer.FreezeObserver;

/**
 * 子弹基类
 * @author hitsz
 */
public abstract class BaseBullet extends AbstractFlyingObject implements BombObserver, FreezeObserver {

    private int power = 10;
    protected boolean isFrozen = false;
    private int freezeTicks;


    public BaseBullet(int locationX, int locationY, int speedX, int speedY, int power) {
        super(locationX, locationY, speedX, speedY);
        this.power = power;
    }

    @Override
    public void forward() {
        if(isFrozen) {
            return;
        }

        super.forward();

        // 判定 x 轴出界
        if (locationX <= 0 || locationX >= Main.WINDOW_WIDTH) {
            vanish();
        }

        // 判定 y 轴出界
        if (speedY > 0 && locationY >= Main.WINDOW_HEIGHT ) {
            // 向下飞行出界
            vanish();
        }else if (locationY <= 0){
            // 向上飞行出界
            vanish();
        }
    }

    public int getPower() {
        return power;
    }

    @Override
    public void onBombExplosion() {
        this.vanish();
    }

    /** 应用 5 秒的子弹冰冻，刷新效果时长。 */
    @Override
    public void onFreezeStart() {
        this.isFrozen = true;
        freezeTicks = 5_000 / Main.TICK_DURATION_MS;
    }

    /** 游戏线程每步推进一次计时，倒计时结束后允许子弹继续移动。 */
    public void tickEffects() {
        if (freezeTicks > 0 && --freezeTicks == 0) {
            isFrozen = false;
        }
    }
}

