package enemySpawnStrategy;

import aircraft.AbstractEnemyAircraft;
import facroty.EnemyGenerator;

import java.util.LinkedList;
import java.util.List;

public class NormalSpawn extends AbstractSpawnStrategy{
    private static EnemyGenerator generator = EnemyGenerator.getInstance();

    @Override
    public List<AbstractEnemyAircraft> spawnEnemy(int difficultyFactor) {
        List<AbstractEnemyAircraft> result = new LinkedList<>();
        result.add(generator.generateEnemy(difficultyFactor));
        return result;
    }


}
