package com.benk97.entities;

import aurelienribon.tweenengine.*;
import aurelienribon.tweenengine.equations.Linear;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.benk97.Settings;
import com.benk97.assets.Assets;
import com.benk97.components.*;

import java.util.Random;

import static com.badlogic.gdx.graphics.g2d.Animation.PlayMode.LOOP;
import static com.benk97.SpaceKillerGameConstants.*;
import static com.benk97.assets.Assets.GFX_LEVEL1_ATLAS;
import static com.benk97.assets.Assets.SOUND_FIRE_ENEMY;
import static com.benk97.components.Mappers.position;
import static com.benk97.components.Mappers.sprite;
import static com.benk97.components.PlayerComponent.PowerLevel.DOUBLE;
import static com.benk97.components.PlayerComponent.PowerLevel.NORMAL;
import static com.benk97.tweens.PositionComponentAccessor.POSITION_Y;
import static com.benk97.tweens.SpriteComponentAccessor.ALPHA;

public class EntityFactory {

    private PooledEngine engine;
    private Assets assets;
    private TweenManager tweenManager;
    private TextureAtlas atlas;
    private Random random = new Random(System.currentTimeMillis());

    public EntityFactory(PooledEngine engine, Assets assets, TweenManager tweenManager) {
        this.engine = engine;
        this.assets = assets;
        this.tweenManager = tweenManager;
        this.atlas = assets.get(GFX_LEVEL1_ATLAS);
    }


    public Entity createBackground(Texture texture, float velocity) {
        Entity background = engine.createEntity();
        BackgroundComponent component = engine.createComponent(BackgroundComponent.class);
        component.setTexture(texture);
        background.add(component);
        background.add(engine.createComponent(PositionComponent.class));
        background.add(engine.createComponent(VelocityComponent.class));
        background.getComponent(VelocityComponent.class).y = velocity;
        engine.addEntity(background);
        return background;
    }

    public Entity createPlayerFire(Entity player) {
        Entity bullet = engine.createEntity();
        bullet.add(engine.createComponent(PlayerBulletComponent.class));
        PositionComponent positionComponent = engine.createComponent(PositionComponent.class);
        bullet.add(positionComponent);
        VelocityComponent velocityComponent = engine.createComponent(VelocityComponent.class);
        bullet.add(velocityComponent);
        SpriteComponent spriteComponent = engine.createComponent(SpriteComponent.class);
        PlayerComponent playerComponent = Mappers.player.get(player);
        spriteComponent.sprite = new Sprite(playerComponent.powerLevel.equals(NORMAL) ? atlas.findRegion("bullet")
                : playerComponent.powerLevel.equals(DOUBLE) ? atlas.findRegion("bullet2") : atlas.findRegion("bullet3"));
        bullet.add(spriteComponent);
        bullet.add(engine.createComponent(RemovableComponent.class));
        engine.addEntity(bullet);
        PositionComponent playerPosition = position.get(player);
        positionComponent.x = playerPosition.x + Mappers.sprite.get(player).sprite.getWidth() / 2f - spriteComponent.sprite.getWidth() / 2f;
        positionComponent.y = playerPosition.y + sprite.get(player).sprite.getHeight();
        velocityComponent.y = PLAYER_BULLET_VELOCITY;
        return bullet;
    }

    public Entity createEnemyFire(Entity enemy, Entity player) {
        assets.playSound(SOUND_FIRE_ENEMY);
        Entity bullet = engine.createEntity();
        bullet.add(engine.createComponent(EnemyBulletComponent.class));
        PositionComponent positionComponent = engine.createComponent(PositionComponent.class);
        bullet.add(positionComponent);
        VelocityComponent velocityComponent = engine.createComponent(VelocityComponent.class);
        bullet.add(velocityComponent);
        SpriteComponent spriteComponent = engine.createComponent(SpriteComponent.class);
        spriteComponent.sprite = new Sprite(atlas.findRegion("bulletEnnemy"));
        bullet.add(spriteComponent);
        bullet.add(engine.createComponent(RemovableComponent.class));
        engine.addEntity(bullet);
        PositionComponent playerPosition = position.get(player);
        PositionComponent enemyPosition = position.get(enemy);
        positionComponent.x = enemyPosition.x + Mappers.sprite.get(enemy).sprite.getWidth() / 2f - spriteComponent.sprite.getWidth() / 2f;
        positionComponent.y = enemyPosition.y + sprite.get(enemy).sprite.getHeight();
        Vector2 directionBullet = new Vector2(playerPosition.x - enemyPosition.x, playerPosition.y - enemyPosition.y);
        directionBullet.nor();
        directionBullet.rotate(-10 + random.nextFloat() * 20f);
        directionBullet.scl(Mappers.enemy.get(enemy).bulletVelocity);
        velocityComponent.x = directionBullet.x;
        velocityComponent.y = directionBullet.y;
        return bullet;
    }


    public Entity createPowerUp(Entity squadron) {
        final Entity powerUp = engine.createEntity();
        powerUp.add(engine.createComponent(PowerUpComponent.class));
        PositionComponent position = engine.createComponent(PositionComponent.class);
        powerUp.add(position);
        powerUp.add(engine.createComponent(VelocityComponent.class));
        AnimationComponent animationComponent = engine.createComponent(AnimationComponent.class);
        Array<Sprite> sprites = atlas.createSprites("power-up");
        animationComponent.animations.put(ANIMATION_MAIN, new Animation<Sprite>(FRAME_DURATION_POWERUP, sprites, LOOP));
        powerUp.add(animationComponent);
        SpriteComponent component = engine.createComponent(SpriteComponent.class);
        component.sprite = sprites.get(0);
        powerUp.add(component);
        position.x = Mappers.squadron.get(squadron).lastKilledPosition.x;
        position.y = Mappers.squadron.get(squadron).lastKilledPosition.y;
        powerUp.add(engine.createComponent(StateComponent.class));
        Timeline.createSequence()
                .beginParallel()
                .push(Tween.to(position, POSITION_Y, 8f).ease(Linear.INOUT).target(50f))
                .push(Tween.to(component, ALPHA, 0.5f).delay(4f).ease(Linear.INOUT).target(0f).repeat(8, 0f))
                .end()
                .setCallback(new TweenCallback() {
                    @Override
                    public void onEvent(int i, BaseTween<?> baseTween) {
                        if (i == TweenCallback.COMPLETE) {
                            engine.removeEntity(powerUp);
                        }
                    }
                })
                .start(tweenManager);
        engine.addEntity(powerUp);
        return powerUp;
    }

    public Entity createShieldUp(Entity squadron) {
        final Entity shieldUp = engine.createEntity();
        shieldUp.add(engine.createComponent(ShieldUpComponent.class));
        PositionComponent position = engine.createComponent(PositionComponent.class);
        shieldUp.add(position);
        shieldUp.add(engine.createComponent(VelocityComponent.class));
        AnimationComponent animationComponent = engine.createComponent(AnimationComponent.class);
        Array<Sprite> sprites = atlas.createSprites("shieldup");
        animationComponent.animations.put(ANIMATION_MAIN, new Animation<Sprite>(FRAME_DURATION_POWERUP, sprites, LOOP));
        shieldUp.add(animationComponent);
        SpriteComponent component = engine.createComponent(SpriteComponent.class);
        component.sprite = sprites.get(0);
        shieldUp.add(component);
        position.x = Mappers.squadron.get(squadron).lastKilledPosition.x;
        position.y = Mappers.squadron.get(squadron).lastKilledPosition.y;
        shieldUp.add(engine.createComponent(StateComponent.class));
        Timeline.createSequence()
                .beginParallel()
                .push(Tween.to(position, POSITION_Y, 8f).ease(Linear.INOUT).target(50f))
                .push(Tween.to(component, ALPHA, 0.5f).delay(4f).ease(Linear.INOUT).target(0f).repeat(8, 0f))
                .end()
                .setCallback(new TweenCallback() {
                    @Override
                    public void onEvent(int i, BaseTween<?> baseTween) {
                        if (i == TweenCallback.COMPLETE) {
                            engine.removeEntity(shieldUp);
                        }
                    }
                })
                .start(tweenManager);
        engine.addEntity(shieldUp);
        return shieldUp;
    }


    public Entity createEnemySoucoupe(Entity squadron, boolean canAttack, float velocityBullet) {
        Entity enemy = engine.createEntity();
        EnemyComponent enemyComponent = engine.createComponent(EnemyComponent.class);
        enemyComponent.points = 100;
        enemyComponent.bulletVelocity = velocityBullet;
        enemyComponent.canAttack = canAttack;
        if (squadron != null) {
            enemyComponent.squadron = squadron;
        }
        enemy.add(enemyComponent);
        PositionComponent position = engine.createComponent(PositionComponent.class);
        enemy.add(position);
        enemy.add(engine.createComponent(VelocityComponent.class));
        AnimationComponent animationComponent = engine.createComponent(AnimationComponent.class);
        Array<Sprite> sprites = atlas.createSprites("soucoupe");
        animationComponent.animations.put(ANIMATION_MAIN, new Animation<Sprite>(FRAME_DURATION, sprites, LOOP));
        enemy.add(animationComponent);
        SpriteComponent component = engine.createComponent(SpriteComponent.class);
        component.sprite = sprites.get(0);
        enemy.add(component);
        enemy.add(engine.createComponent(StateComponent.class));
        engine.addEntity(enemy);
        return enemy;
    }


    public Entity createEnemyShip(Entity squadron, boolean canAttack, float velocityBullet) {
        Entity enemy = engine.createEntity();
        EnemyComponent enemyComponent = engine.createComponent(EnemyComponent.class);
        enemyComponent.points = 200;
        enemyComponent.bulletVelocity = velocityBullet;
        enemyComponent.canAttack = canAttack;
        if (squadron != null) {
            enemyComponent.squadron = squadron;
        }
        enemy.add(enemyComponent);
        PositionComponent position = engine.createComponent(PositionComponent.class);
        enemy.add(position);
        enemy.add(engine.createComponent(VelocityComponent.class));
        AnimationComponent animationComponent = engine.createComponent(AnimationComponent.class);
        Array<Sprite> sprites = atlas.createSprites("enemy");
        animationComponent.animations.put(ANIMATION_MAIN, new Animation<Sprite>(FRAME_DURATION, sprites, LOOP));
        enemy.add(animationComponent);
        SpriteComponent component = engine.createComponent(SpriteComponent.class);
        component.sprite = sprites.get(0);
        enemy.add(component);
        enemy.add(engine.createComponent(StateComponent.class));
        engine.addEntity(enemy);
        return enemy;
    }

    public Entity createAsteroid(Entity squadron) {
        Entity enemy = engine.createEntity();
        EnemyComponent enemyComponent = engine.createComponent(EnemyComponent.class);
        enemyComponent.points = 50;
        if (squadron != null) {
            enemyComponent.squadron = squadron;
        }
        enemy.add(enemyComponent);
        PositionComponent position = engine.createComponent(PositionComponent.class);
        enemy.add(position);
        enemy.add(engine.createComponent(VelocityComponent.class));
        AnimationComponent animationComponent = engine.createComponent(AnimationComponent.class);
        Array<Sprite> sprites = atlas.createSprites("asteroid");
        animationComponent.animations.put(ANIMATION_MAIN, new Animation<Sprite>(FRAME_DURATION, sprites, LOOP));
        enemy.add(animationComponent);
        SpriteComponent component = engine.createComponent(SpriteComponent.class);
        component.sprite = sprites.get(0);
        enemy.add(component);
        enemy.add(engine.createComponent(StateComponent.class));
        engine.addEntity(enemy);
        return enemy;
    }

    public Entity createEntityPlayer() {
        Entity player = engine.createEntity();
        PlayerComponent playerComponent = engine.createComponent(PlayerComponent.class);
        playerComponent.setHighScore(Settings.getHighscore());
        player.add(playerComponent);
        player.add(engine.createComponent(PositionComponent.class));
        player.add(engine.createComponent(VelocityComponent.class));
        AnimationComponent animationComponent = engine.createComponent(AnimationComponent.class);
        Array<Sprite> spritesMAIN = new Array<Sprite>(2);
        spritesMAIN.add(atlas.createSprite("player", 1));
        spritesMAIN.add(atlas.createSprite("player", 2));
        Array<Sprite> spritesLEFT = new Array<Sprite>(2);
        spritesLEFT.add(atlas.createSprite("player", 0));
        spritesLEFT.add(atlas.createSprite("player", 3));
        Array<Sprite> spritesRIGHT = new Array<Sprite>(2);
        spritesRIGHT.add(atlas.createSprite("player", 0));
        spritesRIGHT.add(atlas.createSprite("player", 3));
        spritesRIGHT.get(0).flip(true, false);
        spritesRIGHT.get(1).flip(true, false);
        animationComponent.animations.put(ANIMATION_MAIN, new Animation<Sprite>(FRAME_DURATION, spritesMAIN, LOOP));
        animationComponent.animations.put(GO_LEFT, new Animation<Sprite>(FRAME_DURATION, spritesLEFT, LOOP));
        animationComponent.animations.put(GO_RIGHT, new Animation<Sprite>(FRAME_DURATION, spritesRIGHT, LOOP));
        player.add(animationComponent);
        SpriteComponent component = engine.createComponent(SpriteComponent.class);
        component.sprite = spritesMAIN.get(0);
        component.stayInBoundaries = true;
        player.add(component);
        player.add(engine.createComponent(StateComponent.class));
        engine.addEntity(player);
        Mappers.position.get(player).setPosition(PLAYER_ORIGIN_X, PLAYER_ORIGIN_Y);
        return player;
    }

    public Entity createShield(Entity player) {
        final Entity shield = engine.createEntity();
        PositionComponent playerPosition = Mappers.position.get(player);
        SpriteComponent playerSprite = Mappers.sprite.get(player);
        SpriteComponent spriteComponent = engine.createComponent(SpriteComponent.class);
        spriteComponent.sprite = atlas.createSprite("shield");
        PositionComponent positionComponent = engine.createComponent(PositionComponent.class);
        positionComponent.setPosition(playerPosition.x - (spriteComponent.sprite.getWidth() - playerSprite.sprite.getWidth()) / 2f,
                playerPosition.y - (spriteComponent.sprite.getHeight() - playerSprite.sprite.getHeight()) / 2f);
        shield.add(positionComponent);
        shield.add(spriteComponent);
        shield.add(engine.createComponent(ShieldComponent.class));
        engine.addEntity(shield);
        Timeline.createSequence().beginSequence()
                .delay(5f)
                .push(Tween.to(spriteComponent, ALPHA, 0.2f).target(0.2f))
                .push(Tween.to(spriteComponent, ALPHA, 0.2f).target(0.8f))
                .repeat(5, 0f)
                .setCallback(new TweenCallback() {
                    @Override
                    public void onEvent(int i, BaseTween<?> baseTween) {
                        if (i == TweenCallback.COMPLETE) {
                            engine.removeEntity(shield);
                        }
                    }
                })
                .start(tweenManager);
        return shield;
    }

    public Array<Entity> createEntityPlayerLives(Entity player) {
        PlayerComponent playerComponent = Mappers.player.get(player);
        Array<Entity> entities = new Array<Entity>(playerComponent.lives);
        for (int i = 0; i < playerComponent.lives - 1; ++i) {
            Entity life = engine.createEntity();
            SpriteComponent component = engine.createComponent(SpriteComponent.class);
            Sprite sprite = atlas.createSprite("player", 1);
            component.setTexture(sprite, 1f, 0f, 0.5f);
            component.setPosition(LIVES_X + 20f * i, LIVES_Y - sprite.getHeight());
            life.add(component);
            engine.addEntity(life);
            entities.add(life);
        }
        return entities;
    }

    public Entity createEntityExploding(float x, float y) {
        Entity explosion = engine.createEntity();
        PositionComponent position = engine.createComponent(PositionComponent.class);
        explosion.add(position);
        position.setPosition(x, y);
        explosion.add(engine.createComponent(VelocityComponent.class));
        AnimationComponent animationComponent = engine.createComponent(AnimationComponent.class);
        Array<Sprite> sprites = atlas.createSprites("explosion");
        animationComponent.animations.put(ANIMATION_MAIN, new Animation<Sprite>(FRAME_DURATION_EXPLOSION, sprites, Animation.PlayMode.NORMAL));
        animationComponent.playMode = Animation.PlayMode.NORMAL;
        explosion.add(animationComponent);
        SpriteComponent component = engine.createComponent(SpriteComponent.class);
        explosion.add(component);
        explosion.add(engine.createComponent(StateComponent.class));
        engine.addEntity(explosion);
        return explosion;
    }

    public Entity createEntityFireButton(float alpha, float posX, float posY) {
        Entity entity = engine.createEntity();
        SpriteComponent component = engine.createComponent(SpriteComponent.class);
        component.setTexture(atlas.createSprite("fire_button"), alpha, 0, 1f);
        component.setPosition(posX, posY);
        entity.add(component);
        engine.addEntity(entity);
        return entity;

    }

    public Entity createEntitiesPadController(float alpha, float posX, float posY) {
        Entity pad = engine.createEntity();
        SpriteComponent component = engine.createComponent(SpriteComponent.class);
        component.setTexture(atlas.createSprite("pad"), 0.2f, 0f, 1f);
        component.setPosition(posX, posY);
        pad.add(component);
        engine.addEntity(pad);
        return pad;
    }

    public Entity createSquadron(boolean powerUp, boolean displayScoreBonus, int bonus) {
        Entity squadron = engine.createEntity();
        SquadronComponent squadronComponent = engine.createComponent(SquadronComponent.class);
        squadronComponent.powerUpAfterDestruction = powerUp;
        squadronComponent.displayBonusSquadron = displayScoreBonus;
        squadronComponent.scoreBonus = bonus;
        squadron.add(squadronComponent);
        engine.addEntity(squadron);
        return squadron;
    }

    public Entity createScoreSquadron(Entity squadron) {
        Entity scoreSquadron = engine.createEntity();
        ScoreSquadronComponent score = engine.createComponent(ScoreSquadronComponent.class);
        PositionComponent position = engine.createComponent(PositionComponent.class);
        SquadronComponent squadronComponent = Mappers.squadron.get(squadron);
        score.score = squadronComponent.scoreBonus + "";
        position.x = squadronComponent.lastKilledPosition.x;
        position.y = squadronComponent.lastKilledPosition.y;
        scoreSquadron.add(position);
        scoreSquadron.add(score);
        engine.addEntity(scoreSquadron);
        return scoreSquadron;
    }
}