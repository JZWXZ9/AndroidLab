package shootStrategy;

import aircraft.AbstractAircraft;
import aircraft.HeroAircraft;
import bullet.BaseBullet;
import bullet.EnemyBullet;
import bullet.HeroBullet;

import java.util.List;

public abstract class AbstractShootStrategy {
    protected AbstractAircraft aircraft;

    public AbstractShootStrategy(AbstractAircraft aircraft) {
        this.aircraft = aircraft;
    }

    public abstract List<BaseBullet> shootStrategy();

    public BaseBullet createHeroOrEnemyBullet(int locationX, int locationY, int speedX, int speedY) {
        return (aircraft instanceof HeroAircraft) ? new HeroBullet(locationX,
                locationY, speedX, speedY, aircraft.getPower())
                : new EnemyBullet(locationX,
                locationY, speedX, speedY, aircraft.getPower());
    }
}
