/** 主要思路：将 Image 类改动成 Bitmap
 * Android 上必须通过 Resources 和 R.drawable.* 解码为 Bitmap，并关闭密度自动缩放以保证原始像素与碰撞框一致。
 * */
package application;


import aircraft.*;
import bullet.EnemyBullet;
import bullet.EnemyTrackingBullet;
import bullet.HeroBullet;

import edu.hitsz.aircraftwar.android.R;
import props.*;

/* 改动： Step 2 ImageManager */
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;


import java.util.HashMap;
import java.util.Map;

/**
 * 综合管理图片的加载，访问
 * 提供图片的静态访问方法
 *
 * @author hitsz
 */
public class ImageManager {

    /**
     * 类名-图片 映射，存储各基类的图片
     * 可使用 CLASSNAME_IMAGE_MAP.get( obj.getClass().getName() ) 获得 obj 所属基类对应的图片
     */
    private static final Map<String, Bitmap> CLASSNAME_IMAGE_MAP = new HashMap<>();

    public static Bitmap PEACEFUL_BACKGROUND_IMAGE;
    public static Bitmap EASY_BACKGROUND_IMAGE;
    public static Bitmap NORMAL_BACKGROUND_IMAGE;
    public static Bitmap HARD_BACKGROUND_IMAGE;

    public static Bitmap HERO_BULLET_IMAGE;
    public static Bitmap ENEMY_BULLET_IMAGE;
    public static Bitmap ENEMY_TRACKING_BULLET_IMAGE;

    public static Bitmap HERO_IMAGE;
    public static Bitmap MOB_ENEMY_IMAGE;
    public static Bitmap ELITE_MOB_ENEMY_IMAGE;
    public static Bitmap ELITE_PRO_MOB_ENEMY_IMAGE;
    public static Bitmap ELITE_PLUS_MOB_ENEMY_IMAGE;
    public static Bitmap BOSS_ENEMY_IMAGE;

    public static Bitmap BLOOD_PROP;
    public static Bitmap BOMB_PROP;
    public static Bitmap BULLET_PROP;
    public static Bitmap BULLET_PLUS_PROP;
    public static Bitmap FREEZE_PROP;

    /* 改动： 将原先的 static 块删除，改为在 Surface 创建时显式调用 load() 解码成 Bitmap */
    // loaded 标识图片是否已经加载完成，避免重复解码
    private static boolean loaded;

    public static void load(Resources resources) {
        if (loaded) {
            return;
        }
        /* 文件名只能小写 */
        PEACEFUL_BACKGROUND_IMAGE = decode(resources, R.drawable.bg);
        EASY_BACKGROUND_IMAGE = decode(resources, R.drawable.bg2);
        NORMAL_BACKGROUND_IMAGE = decode(resources, R.drawable.bg3);
        HARD_BACKGROUND_IMAGE = decode(resources, R.drawable.bg4);
        HERO_IMAGE = decode(resources, R.drawable.hero);
        MOB_ENEMY_IMAGE = decode(resources, R.drawable.mob);
        ELITE_MOB_ENEMY_IMAGE = decode(resources, R.drawable.elite);
        ELITE_PRO_MOB_ENEMY_IMAGE = decode(resources, R.drawable.elite_pro);
        ELITE_PLUS_MOB_ENEMY_IMAGE = decode(resources, R.drawable.elite_plus);
        BOSS_ENEMY_IMAGE = decode(resources, R.drawable.boss);
        HERO_BULLET_IMAGE = decode(resources, R.drawable.bullet_hero);
        ENEMY_BULLET_IMAGE = decode(resources, R.drawable.bullet_enemy);
        ENEMY_TRACKING_BULLET_IMAGE = decode(resources, R.drawable.bullet_enemy);
        BLOOD_PROP = decode(resources, R.drawable.prop_blood);
        BOMB_PROP = decode(resources, R.drawable.prop_bomb);
        BULLET_PROP = decode(resources, R.drawable.prop_bullet);
        BULLET_PLUS_PROP = decode(resources, R.drawable.prop_bullet_plus);
        FREEZE_PROP = decode(resources, R.drawable.prop_freeze);

        // 加载图片
        registerImages();
        loaded = true;
    }

    /**
     * 按资源 ID 把 res/drawable 里的图片解码成 Android Bitmap。
     * BitmapFactory.Options.inScaled = false + DENSITY_NONE 用来关闭系统按屏幕密度的自动缩放，
     * 保证图片宽高就是原始像素尺寸，和 512 × 768 逻辑碰撞框完全一致。
     */
    private static Bitmap decode(Resources resources, int id) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(resources, id, options);
        if (bitmap == null) {
            throw new IllegalStateException("Cannot decode resource " + id);
        }
        bitmap.setDensity(Bitmap.DENSITY_NONE);
        return bitmap;
    }

    private static void registerImages() {
        CLASSNAME_IMAGE_MAP.put(HeroAircraft.class.getName(), HERO_IMAGE);
        CLASSNAME_IMAGE_MAP.put(MobEnemy.class.getName(), MOB_ENEMY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(EliteMobEnemy.class.getName(), ELITE_MOB_ENEMY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(EliteTrackingMobEnemy.class.getName(), ELITE_MOB_ENEMY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(EliteProMobEnemy.class.getName(), ELITE_PRO_MOB_ENEMY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(ElitePlusMobEnemy.class.getName(), ELITE_PLUS_MOB_ENEMY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(BossEnemy.class.getName(), BOSS_ENEMY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(HeroBullet.class.getName(), HERO_BULLET_IMAGE);
        CLASSNAME_IMAGE_MAP.put(EnemyBullet.class.getName(), ENEMY_BULLET_IMAGE);
        CLASSNAME_IMAGE_MAP.put(EnemyTrackingBullet.class.getName(), ENEMY_TRACKING_BULLET_IMAGE);
        CLASSNAME_IMAGE_MAP.put(BloodProps.class.getName(), BLOOD_PROP);
        CLASSNAME_IMAGE_MAP.put(BulletProps.class.getName(), BULLET_PROP);
        CLASSNAME_IMAGE_MAP.put(BulletPlusProps.class.getName(), BULLET_PLUS_PROP);
        CLASSNAME_IMAGE_MAP.put(BombProps.class.getName(), BOMB_PROP);
        CLASSNAME_IMAGE_MAP.put(FreezeProps.class.getName(), FREEZE_PROP);
    }

    public static Bitmap get(String className){
        return CLASSNAME_IMAGE_MAP.get(className);
    }

    public static Bitmap get(Object obj){
        if (obj == null){
            return null;
        }
        return get(obj.getClass().getName());
    }




}
