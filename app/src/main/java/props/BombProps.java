package props;

import aircraft.AbstractEnemyAircraft;
import application.ImageManager;
import bullet.BaseBullet;
import observer.BombObserver;
import observer.BombSubject;
import observer.ConcreteBombSubject;

import java.util.List;

public class BombProps extends AbstractProps {
    private final ConcreteBombSubject bombSubject;

    public BombProps(int locationX, int locationY) {
        super(locationX, locationY);
        imageWidth = (int) ImageManager.BOMB_PROP.getWidth();
        this.bombSubject = new ConcreteBombSubject();
    }

    @Override
    public void functionOnHero() {
        System.out.println("Bomb Props activated!");
    }

    @Override
    public void functionOnEnemy(List<AbstractEnemyAircraft> enemyAircrafts, List<BaseBullet> enemyBullet) {
        // 将所有敌机注册为观察者
        for (AbstractEnemyAircraft enemy : enemyAircrafts) {
            bombSubject.addObserver(enemy);
        }
        for (BaseBullet bullet : enemyBullet) {
            bombSubject.addObserver(bullet);
        }
        bombSubject.trigger();
    }
}