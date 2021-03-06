package ch.logixisland.anuto.entity.tower;

import android.graphics.Canvas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ch.logixisland.anuto.R;
import ch.logixisland.anuto.data.game.EntityDescriptor;
import ch.logixisland.anuto.data.game.MineLayerDescriptor;
import ch.logixisland.anuto.data.map.MapDescriptorRoot;
import ch.logixisland.anuto.data.map.PathDescriptor;
import ch.logixisland.anuto.data.setting.tower.MineLayerSettings;
import ch.logixisland.anuto.data.setting.tower.TowerSettingsRoot;
import ch.logixisland.anuto.engine.logic.GameEngine;
import ch.logixisland.anuto.engine.logic.entity.Entity;
import ch.logixisland.anuto.engine.logic.entity.EntityFactory;
import ch.logixisland.anuto.engine.logic.entity.EntityListener;
import ch.logixisland.anuto.engine.logic.entity.EntityRegistry;
import ch.logixisland.anuto.engine.render.Layers;
import ch.logixisland.anuto.engine.render.sprite.AnimatedSprite;
import ch.logixisland.anuto.engine.render.sprite.SpriteInstance;
import ch.logixisland.anuto.engine.render.sprite.SpriteTemplate;
import ch.logixisland.anuto.engine.render.sprite.SpriteTransformation;
import ch.logixisland.anuto.engine.render.sprite.SpriteTransformer;
import ch.logixisland.anuto.engine.sound.Sound;
import ch.logixisland.anuto.entity.shot.Mine;
import ch.logixisland.anuto.util.RandomUtils;
import ch.logixisland.anuto.util.math.Line;
import ch.logixisland.anuto.util.math.Vector2;

public class MineLayer extends Tower implements SpriteTransformation {

    private final static String ENTITY_NAME = "mineLayer";
    private final static float ANIMATION_DURATION = 1f;

    public static class Factory implements EntityFactory {
        @Override
        public String getEntityName() {
            return ENTITY_NAME;
        }

        @Override
        public Entity create(GameEngine gameEngine) {
            TowerSettingsRoot towerSettingsRoot = gameEngine.getGameConfiguration().getTowerSettingsRoot();
            MapDescriptorRoot mapDescriptorRoot = gameEngine.getGameConfiguration().getMapDescriptorRoot();
            return new MineLayer(gameEngine, towerSettingsRoot.getMineLayerSettings(), mapDescriptorRoot.getPaths());
        }
    }

    public static class Persister extends TowerPersister {
        public Persister(GameEngine gameEngine, EntityRegistry entityRegistry) {
            super(gameEngine, entityRegistry, ENTITY_NAME);
        }

        @Override
        protected MineLayerDescriptor createEntityDescriptor() {
            return new MineLayerDescriptor();
        }

        @Override
        protected MineLayerDescriptor writeEntityDescriptor(Entity entity) {
            MineLayer mineLayer = (MineLayer) entity;
            MineLayerDescriptor mineLayerDescriptor = (MineLayerDescriptor) super.writeEntityDescriptor(entity);

            Collection<Vector2> minePositions = new ArrayList<>();
            for (Mine mine : mineLayer.mMines) {
                minePositions.add(mine.getTarget());
            }
            mineLayerDescriptor.setMinePositions(minePositions);

            return mineLayerDescriptor;
        }

        @Override
        protected MineLayer readEntityDescriptor(EntityDescriptor entityDescriptor) {
            MineLayer mineLayer = (MineLayer) super.readEntityDescriptor(entityDescriptor);
            MineLayerDescriptor mineLayerDescriptor = (MineLayerDescriptor) entityDescriptor;

            for (Vector2 minePosition : mineLayerDescriptor.getMinePositions()) {
                Mine mine = new Mine(mineLayer, minePosition, mineLayer.getDamage(), mineLayer.mExplosionRadius);
                mineLayer.mMines.add(mine);
                mine.addListener(mineLayer.mMineListener);
                getGameEngine().add(mine);
            }

            return mineLayer;
        }
    }

    private static class StaticData {
        public SpriteTemplate mSpriteTemplate;
    }

    private MineLayerSettings mSettings;
    private Collection<PathDescriptor> mPaths;

    private float mAngle;
    private int mMaxMineCount;
    private float mExplosionRadius;
    private boolean mShooting;
    private Collection<Line> mSections;
    private Collection<Mine> mMines = new ArrayList<>();

    private AnimatedSprite mSprite;
    private Sound mSound;

    private final EntityListener mMineListener = new EntityListener() {
        @Override
        public void entityRemoved(Entity entity) {
            Mine mine = (Mine) entity;
            mine.removeListener(this);
            mMines.remove(mine);
        }
    };

    private MineLayer(GameEngine gameEngine, MineLayerSettings settings, Collection<PathDescriptor> paths) {
        super(gameEngine, settings);
        StaticData s = (StaticData) getStaticData();

        mPaths = paths;
        mSettings = settings;

        mSprite = getSpriteFactory().createAnimated(Layers.TOWER_BASE, s.mSpriteTemplate);
        mSprite.setListener(this);
        mSprite.setSequenceForwardBackward();
        mSprite.setInterval(ANIMATION_DURATION);

        mAngle = RandomUtils.next(360f);
        mMaxMineCount = mSettings.getMaxMineCount();
        mExplosionRadius = mSettings.getExplosionRadius();

        mSound = getSoundFactory().createSound(R.raw.gun2_donk);
    }

    @Override
    public String getEntityName() {
        return ENTITY_NAME;
    }

    @Override
    public Object initStatic() {
        StaticData s = new StaticData();

        s.mSpriteTemplate = getSpriteFactory().createTemplate(R.attr.mineLayer, 6);
        s.mSpriteTemplate.setMatrix(1f, 1f, null, null);

        return s;
    }

    @Override
    public void init() {
        super.init();

        getGameEngine().add(mSprite);
    }

    @Override
    public void clean() {
        super.clean();

        getGameEngine().remove(mSprite);

        for (Mine m : mMines) {
            m.removeListener(mMineListener);
            m.remove();
        }

        mMines.clear();
    }

    @Override
    public void setPosition(Vector2 position) {
        super.setPosition(position);
        mSections = getPathSectionsInRange(mPaths);
    }

    @Override
    public void move(Vector2 offset) {
        super.move(offset);
        mSections = getPathSectionsInRange(mPaths);
    }

    @Override
    public void enhance() {
        super.enhance();
        mMaxMineCount += mSettings.getEnhanceMaxMineCount();
        mExplosionRadius += mSettings.getEnhanceExplosionRadius();
    }

    @Override
    public void tick() {
        super.tick();

        if (isReloaded() && mMines.size() < mMaxMineCount && mSections.size() > 0) {
            mShooting = true;
            setReloaded(false);
        }

        if (mShooting) {
            mSprite.tick();

            if (mSprite.getSequenceIndex() == 5) {
                Mine m = new Mine(this, getPosition(), getTarget(), getDamage(), mExplosionRadius);
                m.addListener(mMineListener);
                mMines.add(m);
                getGameEngine().add(m);
                mSound.play();

                mShooting = false;
            }
        }

        if (mSprite.getSequenceIndex() != 0) {
            mSprite.tick();
        }
    }

    @Override
    public void draw(SpriteInstance sprite, SpriteTransformer transformer) {
        transformer.translate(getPosition());
        transformer.rotate(mAngle);
    }

    @Override
    public void preview(Canvas canvas) {
        mSprite.draw(canvas);
    }

    @Override
    public List<TowerInfoValue> getTowerInfoValues() {
        List<TowerInfoValue> properties = new ArrayList<>();
        properties.add(new TowerInfoValue(R.string.damage, getDamage()));
        properties.add(new TowerInfoValue(R.string.splash, mExplosionRadius));
        properties.add(new TowerInfoValue(R.string.reload, getReloadTime()));
        properties.add(new TowerInfoValue(R.string.range, getRange()));
        properties.add(new TowerInfoValue(R.string.inflicted, getDamageInflicted()));
        return properties;
    }

    private Vector2 getTarget() {
        float totalLen = 0f;

        for (Line section : mSections) {
            totalLen += section.length();
        }

        float dist = RandomUtils.next(totalLen);

        for (Line section : mSections) {
            float length = section.length();

            if (dist > length) {
                dist -= length;
            } else {
                return section.lineVector()
                        .norm()
                        .mul(dist)
                        .add(section.getPoint1());
            }
        }

        return null;
    }
}
