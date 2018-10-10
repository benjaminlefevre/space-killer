/*
 * Developed by Benjamin Lefèvre
 * Last modified 08/10/18 08:06
 * Copyright (c) 2018. All rights reserved.
 */

package com.bendk97.screens.levels;

import aurelienribon.tweenengine.*;
import box2dLight.RayHandler;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.*;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.SnapshotArray;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.bendk97.Settings;
import com.bendk97.SpaceKillerGame;
import com.bendk97.assets.Assets;
import com.bendk97.components.*;
import com.bendk97.entities.EntityFactory;
import com.bendk97.inputs.GestureHandler;
import com.bendk97.inputs.RetroPadController;
import com.bendk97.inputs.VirtualPadController;
import com.bendk97.listeners.PlayerListener;
import com.bendk97.listeners.impl.CollisionListenerImpl;
import com.bendk97.listeners.impl.InputListenerImpl;
import com.bendk97.listeners.impl.PlayerListenerImpl;
import com.bendk97.mask.SpriteMaskFactory;
import com.bendk97.player.PlayerData;
import com.bendk97.screens.MenuScreen;
import com.bendk97.screens.levels.utils.ScreenShake;
import com.bendk97.systems.*;
import com.bendk97.timer.PausableTimer;
import com.bendk97.tweens.CameraTween;
import com.bendk97.tweens.PositionComponentAccessor;
import com.bendk97.tweens.SpriteComponentAccessor;
import com.bendk97.tweens.VelocityComponentAccessor;
import com.bitfire.postprocessing.PostProcessor;
import com.bitfire.postprocessing.effects.MotionBlur;
import com.bitfire.utils.ShaderLoader;

import static com.bendk97.SpaceKillerGameConstants.*;
import static com.bendk97.google.Achievement.*;
import static com.bendk97.screens.levels.Levels.*;
import static com.bendk97.tweens.SpriteComponentAccessor.ALPHA;

public final class LevelScreen extends ScreenAdapter {

    private float time;
    private InputMultiplexer inputProcessor = null;
    private Music music = null;
    private final LevelScript levelScript;
    private final Viewport viewport;
    private final SpriteBatch batcher;
    private final Viewport viewportHUD;
    private final OrthographicCamera cameraHUD;
    private final SpriteBatch batcherHUD;
    private final PooledEngine engine;
    private final EntityFactory entityFactory;
    private final TweenManager tweenManager;
    private final Assets assets;
    private final SpaceKillerGame game;
    private final Entity player;
    private final SpriteMaskFactory spriteMaskFactory;

    private World world;
    private RayHandler rayHandler;
    private final boolean fxLightEnabled;
    private PostProcessor postProcessor;

    private PlayerListenerImpl playerListener;

    private final Levels level;
    private State state = State.RUNNING;

    public enum State {
        PAUSED, RUNNING

    }

    public LevelScreen(final Assets assets, final SpaceKillerGame game, final Levels level) {
        PausableTimer.instance().stop();
        PausableTimer.instance().start();
        this.level = level;
        this.spriteMaskFactory = new SpriteMaskFactory();
        new Thread(new Runnable() {
            @Override
            public void run() {
                spriteMaskFactory.addMask(assets.get(level.getSprites()).getTextures().first());
            }
        }).start();

        this.game = game;
        this.fxLightEnabled = Settings.isLightFXEnabled();
        OrthographicCamera camera = new OrthographicCamera();
        viewport = new ExtendViewport(SCREEN_WIDTH, SCREEN_HEIGHT, camera);
        camera.setToOrtho(false, SCREEN_WIDTH, SCREEN_HEIGHT);
        this.batcher = new SpriteBatch();
        this.cameraHUD = new OrthographicCamera();
        viewportHUD = new ExtendViewport(SCREEN_WIDTH, SCREEN_HEIGHT, cameraHUD);
        cameraHUD.setToOrtho(false, SCREEN_WIDTH, SCREEN_HEIGHT);
        this.batcherHUD = new SpriteBatch();
        this.assets = assets;
        this.tweenManager = new TweenManager();
        ScreenShake screenShake = new ScreenShake(tweenManager, camera);
        engine = new PooledEngine(50, 150, 50, 150);
        engine.addEntityListener(new EntityListener() {
            @Override
            public void entityAdded(Entity entity) {
                if (DEBUG) {
                    Gdx.app.log("entity added", "size: " + engine.getEntities().size());
                }
            }

            @Override
            public void entityRemoved(Entity entity) {
                if (DEBUG) {
                    Gdx.app.log("entity removed", "size " + engine.getEntities().size());
                }
            }
        });
        if (fxLightEnabled) {
            world = new World(new Vector2(0, 0), false);
            rayHandler = new RayHandler(world, Gdx.graphics.getWidth() / 16, Gdx.graphics
                    .getHeight() / 16);
            rayHandler.setShadows(false);
            rayHandler.setCombinedMatrix(camera);
        }
        entityFactory = new EntityFactory(game, engine, assets, tweenManager, rayHandler, screenShake, level);
        player = entityFactory.createEntityPlayer(level);
        SnapshotArray<Entity> lives = entityFactory.createEntityPlayerLives(player);
        SnapshotArray<Entity> bombs = entityFactory.createEntityPlayerBombs(player);
        createSystems(player, lives, bombs, batcher, screenShake);
        registerTweensAccessor();
        registerPostProcessingEffects();
        this.levelScript = getLevelScript(level, assets, entityFactory, tweenManager, player, engine, camera);
        time = -3;
    }


    public void nextLevel() {
        PlayerComponent playerComponent = Mappers.player.get(player);
        FrameBuffer screenshot = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
        screenshot.begin();
        this.render(Gdx.graphics.getDeltaTime());
        screenshot.end();
        playerComponent.level = nextLevelAfter(playerComponent.level);
        PlayerData playerData = playerComponent.copyPlayerData();
        this.dispose();
        switch (level) {
            case Level2:
                game.playServices.unlockAchievement(KILL_BOSS_2);
                game.goToLevelScreen(Level3, playerData, screenshot);
                break;
            case Level3:
                game.playServices.unlockAchievement(KILL_BOSS_3);
                game.goToLevelScreen(Level1, playerData, screenshot);
                break;
            case Level1:
            default:
                game.playServices.unlockAchievement(KILL_BOSS);
                game.goToLevelScreen(Level2, playerData, screenshot);
                break;
        }
    }

    public InputProcessor getGameOverInputProcessor() {
        return new com.bendk97.inputs.GameOverTouchInputProcessor(cameraHUD, game, assets, player);
    }

    private InputProcessor getPauseInputProcessor() {
        return new com.bendk97.inputs.PauseInputProcessor(cameraHUD, this);
    }

    public void continueWithExtraLife() {
        final PlayerComponent playerComponent = Mappers.player.get(player);
        player.remove(GameOverComponent.class);
        this.time = playerComponent.secondScript;
        this.levelScript.initSpawns();
        playerComponent.lives = LIVES;
        playerComponent.bombs = BOMBS;
        playerComponent.resetScore();
        playerListener.updateLivesAndBombsAfterContinue(player);
        playerComponent.numberOfContinue--;
        Mappers.position.get(player).setPosition(PLAYER_ORIGIN_X, PLAYER_ORIGIN_Y);
        Mappers.sprite.get(player).sprite.setPosition(PLAYER_ORIGIN_X, PLAYER_ORIGIN_Y);
        entityFactory.addInvulnerableComponent(player);
        Gdx.input.setInputProcessor(inputProcessor);
        Timeline.createSequence()
                .push(Tween.to(Mappers.sprite.get(player), ALPHA, 0.2f).target(0f))
                .push(Tween.to(Mappers.sprite.get(player), ALPHA, 0.2f).target(1f))
                .repeat(10, 0f)
                .setCallback(new TweenCallback() {
                    @Override
                    public void onEvent(int i, BaseTween<?> baseTween) {
                        if (i == TweenCallback.COMPLETE) {
                            entityFactory.removeInvulnerableComponent(player);
                        }
                    }
                })
                .start(tweenManager);
    }

    public float getCurrentTimeScript() {
        return time;
    }


    private void registerPostProcessingEffects() {
        ShaderLoader.BasePath = "data/shaders/";
        postProcessor = new PostProcessor(false, false, Gdx.app.getType() == Application.ApplicationType.Desktop);
        MotionBlur motionBlur = new MotionBlur();
        motionBlur.setBlurOpacity(0.5f);
        postProcessor.addEffect(motionBlur);
    }

    private void registerTweensAccessor() {
        Tween.registerAccessor(SpriteComponent.class, new SpriteComponentAccessor());
        Tween.registerAccessor(PositionComponent.class, new PositionComponentAccessor());
        Tween.registerAccessor(VelocityComponent.class, new VelocityComponentAccessor());
        Tween.registerAccessor(OrthographicCamera.class, new CameraTween());
    }

    protected void createSystems(Entity player, SnapshotArray<Entity> lives, SnapshotArray<Entity> bombs, SpriteBatch batcher,
                                 ScreenShake screenShake) {
        playerListener = new PlayerListenerImpl(game, assets, entityFactory, lives, bombs, tweenManager, screenShake, this);
        engine.addSystem(playerListener);
        engine.addSystem(createInputHandlerSystem(player, playerListener));
        CollisionListenerImpl collisionListener = new CollisionListenerImpl(tweenManager, screenShake, assets, entityFactory, playerListener, this);
        engine.addSystem(collisionListener);
        engine.addSystem(new AnimationSystem(0));
        engine.addSystem(new BombExplosionSystem(0, collisionListener, player));
        engine.addSystem(new StateSystem(1));
        engine.addSystem(new MovementSystem(2));
        engine.addSystem(new ShieldSystem(3, player));
        // RENDERING
        engine.addSystem(new BatcherBeginSystem(viewport, batcher, 4));
        engine.addSystem(new BackgroundRenderingSystem(batcher, 5));
        engine.addSystem(new DynamicEntitiesRenderingSystem(batcher, 6));
        engine.addSystem(new ScoreSquadronSystem(6, assets, batcher));
        engine.addSystem(new BatcherEndSystem(batcher, 7));
        engine.addSystem(new BatcherHUDBeginSystem(viewportHUD, batcherHUD, 8));
        engine.addSystem(new StaticEntitiesRenderingSystem(batcherHUD, 9));
        engine.addSystem(new ScoresRenderingSystem(batcherHUD, assets, 11));
        engine.addSystem(new GameOverRenderingSystem(batcherHUD, cameraHUD, assets, 10));
        engine.addSystem(new PauseRenderingSystem(batcherHUD, cameraHUD, assets, 10));
        engine.addSystem(new LevelFinishedRenderingSystem(batcherHUD, assets, level, 10));
        if (DEBUG) {
            engine.addSystem(new FPSDisplayRenderingSystem(this, batcherHUD, 11));
        }
        engine.addSystem(new BatcherHUDEndSystem(batcherHUD, 12));
        // END RENDERING
        engine.addSystem(new CollisionSystem(collisionListener, spriteMaskFactory, 13));
        engine.addSystem(new TankAttackSystem(13));
        engine.addSystem(new EnemyAttackSystem(14, entityFactory));
        engine.addSystem(new BossAttackSystem(14, entityFactory));
        engine.addSystem(new SquadronSystem(level, 15, entityFactory, player, playerListener));
        engine.addSystem(new RemovableSystem(16));
    }


    private InputListenerImpl createInputHandlerSystem(Entity player, PlayerListener playerListener) {
        // input
        inputProcessor = new InputMultiplexer();
        inputProcessor.addProcessor(new GestureDetector(new GestureHandler(this, cameraHUD)));

        InputListenerImpl inputListener = new InputListenerImpl(player, playerListener, entityFactory, assets, Settings.isVirtualPad());
        Entity bombButton = entityFactory.createEntityBombButton(0.2f, BOMB_X, BOMB_Y);
        if (!Settings.isVirtualPad()) {
            Entity fireButton = entityFactory.createEntityFireButton(0.2f, FIRE_X, FIRE_Y);
            Entity padController = entityFactory.createEntitiesPadController(0.2f, 1.4f, PAD_X, PAD_Y);
            // define touch area as rectangles
            Sprite padSprite = Mappers.sprite.get(padController).sprite;
            float heightTouch = padSprite.getHeight() * 1.2f / 3f;
            float widthTouch = padSprite.getWidth() * 1.2f / 3f;
            Rectangle[] squareTouchesDirection = new Rectangle[8];
            squareTouchesDirection[0] = new Rectangle(PAD_X, PAD_Y + 2 * heightTouch, widthTouch, heightTouch);
            squareTouchesDirection[1] = new Rectangle(PAD_X + widthTouch, PAD_Y + 2 * heightTouch, widthTouch, heightTouch);
            squareTouchesDirection[2] = new Rectangle(PAD_X + 2 * widthTouch, PAD_Y + 2 * heightTouch, widthTouch, heightTouch);
            squareTouchesDirection[3] = new Rectangle(PAD_X, PAD_Y + heightTouch, widthTouch, heightTouch);
            squareTouchesDirection[4] = new Rectangle(PAD_X + 2 * widthTouch, PAD_Y + heightTouch, widthTouch, heightTouch);
            squareTouchesDirection[5] = new Rectangle(PAD_X, PAD_Y, widthTouch, heightTouch);
            squareTouchesDirection[6] = new Rectangle(PAD_X + widthTouch, PAD_Y, widthTouch, heightTouch);
            squareTouchesDirection[7] = new Rectangle(PAD_X + 2 * widthTouch, PAD_Y, widthTouch, heightTouch);

            inputProcessor.addProcessor(new RetroPadController(this, inputListener, cameraHUD, squareTouchesDirection,
                    Mappers.sprite.get(fireButton).getBounds(),
                    Mappers.sprite.get(bombButton).getBounds()));

        } else {
            Mappers.sprite.get(bombButton).sprite.setY(BOMB_Y_VIRTUAL);
            inputProcessor.addProcessor(new VirtualPadController(this, inputListener, cameraHUD, player,
                    Mappers.sprite.get(bombButton).getBounds()));

        }
        Gdx.input.setInputProcessor(inputProcessor);
        return inputListener;
    }

    @Override
    public void render(float delta) {
        float deltaState = state.equals(State.RUNNING) ? delta : 0f;
        updateScriptLevel(deltaState);
        tweenManager.update(deltaState);
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        postProcessor.capture();
        engine.update(deltaState);
        postProcessor.render();
        if (fxLightEnabled) {
            rayHandler.updateAndRender();
        }
    }

    private void updateScriptLevel(float delta) {
        int timeBefore = (int) Math.floor(time);
        time += delta;
        int newTime = (int) Math.floor(time);
        if (newTime > timeBefore) {
            levelScript.script(newTime);
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        viewportHUD.update(width, height, true);
    }

    @Override
    public void pause() {
        if (player.getComponent(GameOverComponent.class) == null
                && player.getComponent(PauseComponent.class) == null) {
            state = State.PAUSED;
            Gdx.input.setInputProcessor(this.getPauseInputProcessor());
            if (music != null) {
                music.pause();
            }
            PausableTimer.pause();
            player.add(engine.createComponent(PauseComponent.class));
        }
    }

    public void resumeGame() {
        state = State.RUNNING;
        Gdx.input.setInputProcessor(inputProcessor);
        if (music != null) {
            music.play();
        }
        PausableTimer.resume();
        player.remove(PauseComponent.class);
        postProcessor.rebind();
    }

    public void quitGame() {
        this.dispose();
        game.goToScreen(MenuScreen.class);
    }

    @Override
    public void dispose() {
        PausableTimer.instance().stop();
        batcher.dispose();
        batcherHUD.dispose();
        entityFactory.dispose();
        spriteMaskFactory.clear();
        if (fxLightEnabled) {
            rayHandler.dispose();
            world.dispose();
        }
        assets.unloadResources(this.level);
        engine.clearPools();
        engine.removeAllEntities();
        postProcessor.dispose();
    }

    public void submitScore(int scoreInt) {
        game.playServices.submitScore(scoreInt);
    }

    public void checkAchievements(Entity player) {
        PlayerComponent playerComponent = Mappers.player.get(player);
        if (playerComponent.enemiesKilled == 50) {
            game.playServices.unlockAchievement(KILL_50_ENEMIES);
        } else if (playerComponent.enemiesKilled == 100) {
            game.playServices.unlockAchievement(KILL_100_ENEMIES);
        } else if (playerComponent.enemiesKilled == 500) {
            game.playServices.unlockAchievement(KILL_500_ENEMIES);
        }
        if (playerComponent.laserShipKilled == 1) {
            game.playServices.unlockAchievement(KILL_LASER_SHIP);
        } else if (playerComponent.laserShipKilled == 5) {
            game.playServices.unlockAchievement(KILL_5_LASER_SHIPS);
        }
    }
}