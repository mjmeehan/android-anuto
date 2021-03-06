package ch.logixisland.anuto.business.tower;

import ch.logixisland.anuto.business.game.GameState;
import ch.logixisland.anuto.business.game.GameStateListener;
import ch.logixisland.anuto.business.score.CreditsListener;
import ch.logixisland.anuto.business.score.ScoreBoard;
import ch.logixisland.anuto.engine.logic.GameEngine;
import ch.logixisland.anuto.engine.logic.entity.Entity;
import ch.logixisland.anuto.engine.logic.entity.EntityListener;
import ch.logixisland.anuto.engine.logic.loop.Message;
import ch.logixisland.anuto.entity.Types;
import ch.logixisland.anuto.entity.tower.Tower;
import ch.logixisland.anuto.entity.tower.TowerListener;
import ch.logixisland.anuto.util.math.Vector2;

public class TowerSelector implements CreditsListener, GameStateListener, EntityListener, TowerListener {

    private final GameEngine mGameEngine;
    private final GameState mGameState;
    private final ScoreBoard mScoreBoard;

    private TowerInfoView mTowerInfoView;
    private TowerBuildView mTowerBuildView;

    private TowerInfo mTowerInfo;
    private Tower mSelectedTower;

    public TowerSelector(GameEngine gameEngine, GameState gameState, ScoreBoard scoreBoard) {
        mGameEngine = gameEngine;
        mGameState = gameState;
        mScoreBoard = scoreBoard;

        mScoreBoard.addCreditsListener(this);
        mGameState.addListener(this);
    }

    public void setTowerInfoView(TowerInfoView view) {
        mTowerInfoView = view;
    }

    public void setTowerBuildView(TowerBuildView view) {
        mTowerBuildView = view;
    }

    public boolean isTowerSelected() {
        return mSelectedTower != null;
    }

    public TowerInfo getTowerInfo() {
        return mTowerInfo;
    }

    public void requestBuildTower() {
        if (mGameEngine.isThreadChangeNeeded()) {
            mGameEngine.post(new Message() {
                @Override
                public void execute() {
                    requestBuildTower();
                }
            });
            return;
        }

        selectTower(null);
        showTowerBuildView();
    }

    public void selectTowerAt(Vector2 position) {
        if (mGameEngine.isThreadChangeNeeded()) {
            final Vector2 finalPosition = position;
            mGameEngine.post(new Message() {
                @Override
                public void execute() {
                    selectTowerAt(finalPosition);
                }
            });
            return;
        }

        Tower closest = (Tower) mGameEngine
                .getEntitiesByType(Types.TOWER)
                .min(Entity.distanceTo(position));

        if (closest != null && closest.getDistanceTo(position) < 0.6f) {
            selectTower(closest);
        } else {
            selectTower(null);
        }
    }

    public void selectTower(Tower tower) {
        if (mGameEngine.isThreadChangeNeeded()) {
            final Tower finalTower = tower;
            mGameEngine.post(new Message() {
                @Override
                public void execute() {
                    selectTower(finalTower);
                }
            });
            return;
        }

        hideTowerInfoView();
        hideTowerBuildView();

        if (tower != null) {
            if (mSelectedTower == tower) {
                showTowerInfoView();
            } else {
                setSelectedTower(tower);
            }
        } else {
            setSelectedTower(null);
        }
    }

    void showTowerInfo(Tower tower) {
        if (mGameEngine.isThreadChangeNeeded()) {
            mGameEngine.post(new Message() {
                @Override
                public void execute() {
                    showTowerInfoView();
                }
            });
            return;
        }

        setSelectedTower(tower);
        showTowerInfoView();
    }

    void updateTowerInfo() {
        if (mGameEngine.isThreadChangeNeeded()) {
            mGameEngine.post(new Message() {
                @Override
                public void execute() {
                    updateTowerInfo();
                }
            });
            return;
        }

        if (mTowerInfo != null) {
            showTowerInfoView();
        }
    }

    @Override
    public void entityRemoved(Entity entity) {
        selectTower(null);
    }

    @Override
    public void damageInflicted(float totalDamage) {
        updateTowerInfo();
    }

    @Override
    public void valueChanged(int value) {
        updateTowerInfo();
    }

    @Override
    public void creditsChanged(int credits) {
        if (mTowerInfo != null) {
            updateTowerInfo();
        }
    }

    @Override
    public void gameRestart() {
        if (mTowerInfo != null) {
            updateTowerInfo();
        }
    }

    @Override
    public void gameOver() {
        if (mTowerInfo != null) {
            updateTowerInfo();
        }
    }

    Tower getSelectedTower() {
        return mSelectedTower;
    }

    private void setSelectedTower(Tower tower) {
        if (mSelectedTower != null) {
            mSelectedTower.removeListener((TowerListener) this);
            mSelectedTower.removeListener((EntityListener) this);
            mSelectedTower.hideRange();
        }

        mSelectedTower = tower;

        if (mSelectedTower != null) {
            mSelectedTower.addListener((TowerListener) this);
            mSelectedTower.addListener((EntityListener) this);
            mSelectedTower.showRange();
        }
    }

    private void showTowerInfoView() {
        mTowerInfo = new TowerInfo(
                mSelectedTower,
                mScoreBoard.getCredits(),
                mGameState.isGameOver()
        );

        if (mTowerInfoView != null) {
            mTowerInfoView.showTowerInfo(mTowerInfo);
        }
    }

    private void hideTowerInfoView() {
        mTowerInfo = null;

        if (mTowerInfoView != null) {
            mTowerInfoView.hideTowerInfo();
        }
    }

    private void showTowerBuildView() {
        if (mTowerBuildView != null) {
            mTowerBuildView.showTowerBuildView();
        }
    }

    private void hideTowerBuildView() {
        if (mTowerBuildView != null) {
            mTowerBuildView.hideTowerBuildView();
        }
    }

}
