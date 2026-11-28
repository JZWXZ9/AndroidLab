package bullet;

import aircraft.HeroAircraft;
import application.Main;

public class EnemyTrackingBullet extends EnemyBullet{
    private static final int DURATION_TICKS = 5_000 / Main.TICK_DURATION_MS;
    private int ageTicks;
    private static final HeroAircraft hero = HeroAircraft.getInstance();
    private final double CURRENT_SPEED;

    public EnemyTrackingBullet(int locationX, int locationY, int speedX, int speedY, int power) {
        super(locationX, locationY, speedX, speedY, power);
        this.CURRENT_SPEED = Math.hypot(speedX, speedY);
    }

    /** 推进追踪子弹的寿命，并根据英雄当前位置调整飞行方向。 */
    public void update() {
        if (ageTicks >= DURATION_TICKS) {
            vanish();
            return;
        }
        ageTicks++;
        int dx = hero.getLocationX() - getLocationX();
        int dy = hero.getLocationY() - getLocationY();
        double distance = Math.max(1, Math.hypot(dx, dy));
        speedX = (int) (CURRENT_SPEED * dx / distance);
        speedY = (int) (CURRENT_SPEED * dy / distance);
    }

    /** 每步先更新追踪方向，再移动仍有效的子弹。 */
    @Override
    public void forward() {
        if (isFrozen) {
            return;
        }
        update();
        if (!notValid()) {
            super.forward();
        }
    }

}
