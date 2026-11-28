package aircraft;

import application.ImageManager;
import application.Main;
import shootStrategy.FanShapedShoot;
import shootStrategy.ScatterShoot;
import shootStrategy.StraightShoot;

import java.util.List;

/**
 * 英雄飞机，游戏玩家操控
 * @author hitsz
 */
public class HeroAircraft extends AbstractAircraft {
    // 无敌帧，道具相关
    private boolean isInvincible = false;
    // 计算无敌帧时间
    private int invincibleTime = 0;
    // 期望的无敌时间
    private int expectedInvincibleTime = 0;
    private int weaponTicks;

    private volatile static HeroAircraft hero;
    private HeroAircraft() {
        super(
                Main.WINDOW_WIDTH / 2,
                Main.WINDOW_HEIGHT - (int) ImageManager.HERO_IMAGE.getHeight() ,
                0, 0, 500);
        shootNum = 1;
        power = 30;
        direction = -1;
        strategyList.add(new StraightShoot(this));
    }

    // 双重线程锁
    public static HeroAircraft getInstance() {
        if (hero == null) {
            synchronized (HeroAircraft.class) {
                if (hero == null) {
                    hero = new HeroAircraft();
                }
            }
        }
        return hero;
    }

    /** 新建对局时恢复英雄的初始状态。追踪敌机持有同一个英雄实例。 */
    public void resetForGame() {
        maxHp = 500;
        hp = maxHp;
        isValid = true;
        shootNum = 1;
        power = 30;
        direction = -1;
        shootSpeed = 30;
        speedX = 0;
        speedY = 0;
        isInvincible = false;
        invincibleTime = 0;
        expectedInvincibleTime = 0;
        weaponTicks = 0;
        setShootStrategy(List.of(new StraightShoot(this)));
        setLocation(Main.WINDOW_WIDTH / 2, Main.WINDOW_HEIGHT - ImageManager.HERO_IMAGE.getHeight());
    }

    /** 应用火力道具，并从拾取时起重新计满 10 秒游戏时间。 */
    public void activateWeapon(boolean plus) {
        shootNum = Math.min(plus ? 6 : 3, shootNum + 1);
        setShootStrategy(List.of(plus ? new FanShapedShoot(this) : new ScatterShoot(this)));
        weaponTicks = 10_000 / Main.TICK_DURATION_MS;
    }

    /** 由游戏线程每个逻辑步调用一次，更新无敌和火力效果。 */
    public void tickEffects() {
        invincibleController();
        if (weaponTicks > 0 && --weaponTicks == 0) {
            shootNum = 1;
            setShootStrategy(List.of(new StraightShoot(this)));
        }
    }

    @Override
    public void forward() {
        // 英雄机由 BaseGame 在每个逻辑步应用触摸坐标。
    }



    // 加血
    public void increaseHp(int hpIncreasement) {
        hp += hpIncreasement;
        // 满血检测
        if (hp >= maxHp) {
            hp = maxHp;
        }
    }

    public int getMaxHp() {
        return maxHp;
    }

    public void increaseMaxHp(int increasementOfMaxHp) {
        this.maxHp += increasementOfMaxHp;
    }

    public boolean isInvincible() {
        return isInvincible;
    }

    public void setInvincible(boolean isInvincible) {
        this.isInvincible = isInvincible;
    }

    public void setExpectedInvincibleTime(int expectedInvincibleTime) {
        this.expectedInvincibleTime = expectedInvincibleTime;
    }

    public void invincibleController() {
        if(isInvincible) {
            invincibleTime++;
            if (invincibleTime >= expectedInvincibleTime) {
                invincibleTime = 0;
                expectedInvincibleTime = 0;
                isInvincible = false;
            }
        }
    }

    @Override
    public double getAngle() {
        return Math.toDegrees(3 * Math.PI / 2);
    }

    @Override
    public double getAngelIncrement() {
        return Math.PI / 40;
    }

}
