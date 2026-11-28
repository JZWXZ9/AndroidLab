package facroty;

import aircraft.AbstractEnemyAircraft;
import aircraft.HeroAircraft;

import java.util.LinkedList;
import java.util.List;

public class EnemyGenerator {
    public final List<AbstractEnemyFactory> FACTORY = new LinkedList<>();
    private static EnemyGenerator generator;
    /** 为一局游戏创建各类敌机工厂，各工厂在本局内累计生成概率。 */
    public EnemyGenerator() {
        FACTORY.add(new EliteProMobEnemyFactory());
        FACTORY.add(new ElitePlusMobEnemyFactory());
        FACTORY.add(new EliteTrackingMobEnemyFactory());
        FACTORY.add(new EliteMobEnemyFactory());
        FACTORY.add(new MobEnemyFactory());
    }

    public static EnemyGenerator getInstance() {
        if (generator == null) {
            synchronized (HeroAircraft.class) {
                if (generator == null) {
                    generator = new EnemyGenerator();
                }
            }
        }
        return generator;
    }

    public AbstractEnemyAircraft generateEnemy(int difficultyFactor) {
        AbstractEnemyAircraft enemy = null;
        for(AbstractEnemyFactory factory: FACTORY) {
            enemy = factory.createEnemyByProbability(difficultyFactor);
            if (enemy != null) {
                break;
            }

        }
        return enemy;
    }
}
