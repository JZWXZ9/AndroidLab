package props;

import aircraft.AbstractEnemyAircraft;
import application.ImageManager;
import bullet.BaseBullet;
import observer.BombObserver;
import observer.ConcreteFreezeSubject;
import observer.FreezeObserver;
import observer.FreezeSubject;

import java.util.List;

public class FreezeProps extends AbstractProps {
    private final ConcreteFreezeSubject freezeSubject;

    public FreezeProps(int locationX, int locationY) {
        super(locationX, locationY);
        imageWidth = (int) ImageManager.FREEZE_PROP.getWidth();
        this.freezeSubject = new ConcreteFreezeSubject();
    }

    @Override
    public void functionOnHero() {
        return;
    }

    @Override
    public void functionOnEnemy(List<AbstractEnemyAircraft> enemyAircrafts, List<BaseBullet> enemyBullet) {
        // 将所有敌机注册为观察者
        for (AbstractEnemyAircraft enemy : enemyAircrafts) {
            freezeSubject.addObserver(enemy);
        }
        for (BaseBullet bullet : enemyBullet) {
            freezeSubject.addObserver(bullet);
        }

        freezeSubject.trigger();
    }
}