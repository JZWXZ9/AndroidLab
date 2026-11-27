package facroty;

import aircraft.AbstractEnemyAircraft;
import aircraft.EliteMobEnemy;
import application.ImageManager;
import application.Main;

public class EliteMobEnemyFactory extends AbstractEnemyFactory{
    public EliteMobEnemyFactory() {
        this.probabilityForEnemySpawning = 0;
        this.defaultSpawningProbability = 0;
        this.probabilityIncrement = 10;
        this.speedX = 0;
        this.speedY = 10;
        this.hp = 300;
    }

    @Override
    public AbstractEnemyAircraft createEnemy() {
        {
            return new EliteMobEnemy(
                    (int) (random.nextDouble() * (Main.WINDOW_WIDTH - ImageManager.ELITE_MOB_ENEMY_IMAGE.getWidth())),
                    (int) (random.nextDouble() * Main.WINDOW_HEIGHT * 0.05),
                    speedX,
                    speedY,
                    hp);
        }
    }
}
