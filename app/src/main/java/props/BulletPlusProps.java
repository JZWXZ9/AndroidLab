package props;

import aircraft.AbstractEnemyAircraft;
import application.ImageManager;
import bullet.BaseBullet;

import java.util.List;

public class BulletPlusProps extends AbstractProps{
    public BulletPlusProps(int locationX, int locationY) {
        super(locationX, locationY);
        imageWidth = (int) ImageManager.BULLET_PROP.getWidth();
    }

    /** 通知英雄机切换到增强火力道具的扇形射击效果。 */
    @Override
    public void functionOnHero() {
        heroAircraft.activateWeapon(true);
    }

    @Override
    public void functionOnEnemy(List<AbstractEnemyAircraft> enemyAircrafts, List<BaseBullet> enemyBullet) {
        return;
    }
}
