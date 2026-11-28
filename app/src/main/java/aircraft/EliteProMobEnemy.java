package aircraft;

import bullet.BaseBullet;
import bullet.EnemyBullet;
import shootStrategy.FanShapedShoot;
import shootStrategy.ScatterShoot;

import java.util.LinkedList;
import java.util.List;

public class EliteProMobEnemy extends AbstractEnemyAircraft{
    private final int BULLET_TOTAL_SPEED = 10;
    private final int SHOOT_NUM = 3;
    private int maxHp;
    public EliteProMobEnemy(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
        this.maxHp = hp;
        probabilityForEachProp = List.of(100, 50, 25, 10, 10);
        shootCycle = 2;
        score = 1000;
        shootNum = 3;
        shootSpeed = 20;
        power = 15;
        direction = 1;
        strategyList.add(new ScatterShoot(this));
        this.freezeDuration = 3000;
    }

    @Override
    public List<BaseBullet> shoot(){
        List<BaseBullet> res = new LinkedList<>();
        for(int i=0; i< SHOOT_NUM; i++) {
            double angel = (2*i - SHOOT_NUM + 1) * Math.PI / 18;
            EnemyBullet bullet = new EnemyBullet(locationX, locationY,
                    (int) (BULLET_TOTAL_SPEED*Math.sin(angel)), (int)(BULLET_TOTAL_SPEED*Math.cos(angel)), 30);
            res.add(bullet);
        }
        return res;
    }

    @Override
    public int getSpeedXIncrement() {
        return 10;
    }


    @Override
    public void onFreezeStart() {
        // Pro 敌机受到冰冻道具影响时只减速，射击照常进行。
        applyFreeze(true);
    }

    @Override
    public void onBombExplosion() {
        decreaseHp((int)(this.maxHp * 0.5));
    }
}
