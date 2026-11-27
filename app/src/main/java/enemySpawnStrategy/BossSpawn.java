package enemySpawnStrategy;

import aircraft.AbstractEnemyAircraft;
import enums.DifficultyEnums;
import facroty.BossFactory;

import java.util.LinkedList;
import java.util.List;

public class BossSpawn extends AbstractSpawnStrategy{
    private boolean isSpawningSuccessful = false;
    private BossFactory bossGenerator;
    private DifficultyEnums difficulty;

    public BossSpawn(DifficultyEnums difficulty) {
        super();
        this.difficulty = difficulty;
        bossGenerator = new BossFactory(this.difficulty);
    }

    @Override
    public List<AbstractEnemyAircraft> spawnEnemy(int difficultyFactor){
        if(!isSpawningSuccessful) {
            isSpawningSuccessful = true;
            List<AbstractEnemyAircraft> result = new LinkedList<>();
            result.add(bossGenerator.createEnemyByProbability(difficultyFactor));
            return result;
        }
        return List.of();
    }
}
