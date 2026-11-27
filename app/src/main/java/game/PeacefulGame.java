package game;

import application.ImageManager;
import enums.DifficultyEnums;

public class PeacefulGame extends AbstractGame {

    @Override
    protected void initDifficultyParams() {
        this.enemyMaxNumber = 3;
        this.enemySpawnCycle = 35;
        this.heroShootCycle = 1.5;
        this.bossSpawnScore = 0;
        this.allowBoss = false;
        this.difficulty = DifficultyEnums.PEACEFUL;
        this.backGroundImage = ImageManager.PEACEFUL_BACKGROUND_IMAGE;
    }
}