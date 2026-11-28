package aircraft;

import application.Main;
import bullet.BaseBullet;
import observer.BombObserver;
import observer.FreezeObserver;
import props.AbstractProps;
import props.PropsFactory;

import java.util.*;

public abstract class AbstractEnemyAircraft extends AbstractAircraft
        implements BombObserver, FreezeObserver {

    public boolean isFrozen = false;
    private boolean freezeEffectActive;

    // 缓存首次受冻前的速度，解冻时恢复。
    protected int originalSpeedX;
    protected int originalSpeedY;
    protected int score;
    protected int shootCounter = 0;
    protected int shootCycle = 0;
    private int freezeTicks;

    protected long freezeDuration = 0;  // 冰冻时间（毫秒），0 表示永久。
    static final List<String> PROPS_LIST = List.of("BloodProps", "BulletProps", "BulletPlusProps", "BombProps", "FreezeProps");
    protected List<Integer> probabilityForEachProp; // 百分比概率。
    static Random random = new Random();

    public AbstractEnemyAircraft(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);

        // 如果没有就全部初始化为0
        if (probabilityForEachProp == null) {
            probabilityForEachProp = new ArrayList<>();
            for (int i=0; i<PROPS_LIST.size(); i++) {
                probabilityForEachProp.add(0);
            }
        }
    }

    public List<AbstractProps> getPropsIfVanish(){
        List<AbstractProps> props = new LinkedList<>();
        if(PROPS_LIST.size() != probabilityForEachProp.size()) {
            System.out.println(this.getClass().getName() + "has different size between props and probability!");
            return Collections.emptyList();
        }
        for (int i=0; i<PROPS_LIST.size(); i++) {
            if (random.nextInt(100) + 1 <= probabilityForEachProp.get(i)) {
                    props.add(PropsFactory.createProps(PROPS_LIST.get(i), locationX, locationY));
            }
        }
        return props;
    }

    public List<BaseBullet> shootByTick() {
        if(!isFrozen) {
            if (shootCounter >= shootCycle) {
                shootCounter = 0;
                return shoot();
            } else {
                shootCounter++;
                return new LinkedList<>();
            }
        }
        else{
            return List.of();
        }
    }



    public int getScore() {
        return score;
    }

    public int getTrackingBulletCycle() {
        return 0;
    }


    @Override
    public void onBombExplosion() {
        this.vanish();
    }

    /** 冰冻道具通知敌机停止移动和射击。 */
    @Override
    public void onFreezeStart() {
        applyFreeze(false);
    }

    /** 记录原速度并开始冰冻或减速，重复拾取只刷新剩余时间。 */
    protected void applyFreeze(boolean slowOnly) {
        if (!freezeEffectActive) {
            originalSpeedX = speedX;
            originalSpeedY = speedY;
        }
        freezeEffectActive = true;
        isFrozen = !slowOnly;
        speedX = slowOnly ? originalSpeedX / 2 : 0;
        speedY = slowOnly ? originalSpeedY / 2 : 0;
        // 将毫秒换算成 40 ms 逻辑步数；freezeDuration 为 0 时记为 -1，对应原规则中的永久冰冻。
        freezeTicks = freezeDuration > 0 ? (int) (freezeDuration / Main.TICK_DURATION_MS) : -1;
    }

    /** 每步推进冰冻计时，效果结束时恢复首次受影响前的速度。 */
    public void tickEffects() {
        if (freezeTicks > 0 && --freezeTicks == 0) {
            speedX = originalSpeedX;
            speedY = originalSpeedY;
            isFrozen = false;
            freezeEffectActive = false;
        }
    }
}
