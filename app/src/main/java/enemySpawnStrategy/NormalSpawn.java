package enemySpawnStrategy;

import aircraft.AbstractEnemyAircraft;
import facroty.EnemyGenerator;

import java.util.LinkedList;
import java.util.List;

public class NormalSpawn extends AbstractSpawnStrategy{
    // 每局持有自己的生成器，重新开局时恢复初始刷怪概率。
    private final EnemyGenerator generator = new EnemyGenerator();

    @Override
    public List<AbstractEnemyAircraft> spawnEnemy(int difficultyFactor) {
        List<AbstractEnemyAircraft> result = new LinkedList<>();
        result.add(generator.generateEnemy(difficultyFactor));
        return result;
    }


}
