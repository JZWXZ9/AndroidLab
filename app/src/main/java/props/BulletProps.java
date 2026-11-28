package props;

import aircraft.AbstractEnemyAircraft;
import application.ImageManager;
import bullet.BaseBullet;

import java.util.List;

public class BulletProps extends AbstractProps{
    public BulletProps(int locationX, int locationY) {
        super(locationX, locationY);
        imageWidth = (int) ImageManager.BULLET_PROP.getWidth();
    }

    /** 通知英雄机切换到普通火力道具的散射效果。 */
    @Override
    public void functionOnHero() {
        heroAircraft.activateWeapon(false);
    }

    @Override
    public void functionOnEnemy(List<AbstractEnemyAircraft> enemyAircrafts, List<BaseBullet> enemyBullet) {
        return;
    }
}
