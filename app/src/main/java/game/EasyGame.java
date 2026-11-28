package game;

import application.ImageManager;
import enums.DifficultyEnums;

public class EasyGame extends BaseGame  {
    public EasyGame(android.content.Context context) {
        super(context);
    }

    @Override
    protected void initDifficultyParams() {
        this.enemyMaxNumber = 4;
        this.enemySpawnCycle = 28;
        this.heroShootCycle = 1.8;
        this.bossSpawnScore = 0;
        this.allowBoss = false;
        this.difficulty = DifficultyEnums.EASY;
        this.backGroundImage = ImageManager.EASY_BACKGROUND_IMAGE;
    }
}