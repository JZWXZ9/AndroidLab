package aircraft;

import application.Main;

public abstract class AbstractTrackingEnemyAircraft extends AbstractEnemyAircraft{
    private static final int TRACKING_DURATION_TICKS = 1800 / Main.TICK_DURATION_MS;
    private int trackingTicks;
    static final HeroAircraft HERO_AIRCRAFT = HeroAircraft.getInstance();
    private final double TOTAL_SPEED;
    private final int BULLET_SPEED = 20;
    private int trackingBulletCounter;
    private final static int TRACKING_BULLET_CYCLE = 3;

    public AbstractTrackingEnemyAircraft(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
        this.TOTAL_SPEED = Math.hypot(speedX, speedY);
    }

    /** 判断敌机是否还在主动追踪英雄的时间窗口内。 */
    public boolean keepTracking() {
        return trackingTicks < TRACKING_DURATION_TICKS;
    }

    @Override
    public int getTrackingBulletCycle() {
        return TRACKING_BULLET_CYCLE;
    }

    /** 每个逻辑步追踪并移动一次。冻结期间位置和追踪计时一起暂停。 */
    @Override
    public void forward() {
        if (isFrozen) {
            return;
        }
        if (keepTracking()) {
            trackingTicks++;
            double xDistance = HERO_AIRCRAFT.getLocationX() - this.getLocationX();
            double yDistance = HERO_AIRCRAFT.getLocationY() - this.getLocationY();
            // 把指向英雄的向量归一化，保留原飞行速度。中心重合时避免除以零。
            double totalDistance = Math.max(1, Math.hypot(xDistance, yDistance));
            speedX = (int) (TOTAL_SPEED * xDistance / totalDistance);
            speedY = (int) (TOTAL_SPEED * yDistance / totalDistance);
        }
        super.forward();
        if (locationX <= 0 || locationX >= Main.WINDOW_WIDTH || locationY >= Main.WINDOW_HEIGHT) {
            vanish();
        }

    }
}
