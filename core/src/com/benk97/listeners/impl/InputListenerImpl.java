package com.benk97.listeners.impl;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.math.Vector2;
import com.benk97.assets.Assets;
import com.benk97.components.GameOverComponent;
import com.benk97.components.Mappers;
import com.benk97.components.PlayerComponent;
import com.benk97.components.ShieldComponent;
import com.benk97.entities.EntityFactory;
import com.benk97.listeners.InputListener;
import com.benk97.screens.LevelScreen;

import static com.benk97.SpaceKillerGameConstants.*;
import static com.benk97.assets.Assets.SOUND_FIRE;
import static com.benk97.components.Mappers.velocity;

public class InputListenerImpl extends EntitySystem implements InputListener {
    private Family playerFamily = Family.one(PlayerComponent.class).exclude(GameOverComponent.class).get();
    private Family playerShieldFamily = Family.one(ShieldComponent.class).exclude(GameOverComponent.class).get();
    private Entity player;
    private EntityFactory entityFactory;
    private Assets assets;
    private LevelScreen screen;

    public InputListenerImpl(Entity player, EntityFactory entityFactory, Assets assets, LevelScreen screen) {
        this.player = player;
        this.assets = assets;
        this.entityFactory = entityFactory;
        this.screen = screen;
    }


    @Override
    public void fire() {
        if (playerFamily.matches(player)) {
            assets.playSound(SOUND_FIRE, 0.2f);
            entityFactory.createPlayerFire(player);
        }
    }

    @Override
    public void goToMenu() {
        screen.goToMenu();
    }

    @Override
    public void goLeft() {
        velocity.get(player).x = -PLAYER_VELOCITY;
        Mappers.state.get(player).set(GO_LEFT);
    }

    @Override
    public void goRight() {
        velocity.get(player).x = PLAYER_VELOCITY;
        Mappers.state.get(player).set(GO_RIGHT);
    }

    @Override
    public void goTop() {
        velocity.get(player).x = 0;
        velocity.get(player).y = PLAYER_VELOCITY;
        Mappers.state.get(player).set(ANIMATION_MAIN);
    }

    @Override
    public void goDown() {
        velocity.get(player).x = 0;
        velocity.get(player).y = -PLAYER_VELOCITY;
        Mappers.state.get(player).set(ANIMATION_MAIN);
    }

    @Override
    public void goLeftTop() {
        Vector2 vector2 = new Vector2(-1f, 1f);
        vector2.nor();
        velocity.get(player).x = PLAYER_VELOCITY * vector2.x;
        velocity.get(player).y = PLAYER_VELOCITY * vector2.y;
        Mappers.state.get(player).set(GO_LEFT);
    }

    @Override
    public void goLeftDown() {
        Vector2 vector2 = new Vector2(-1f, -1f);
        vector2.nor();
        velocity.get(player).x = PLAYER_VELOCITY * vector2.x;
        velocity.get(player).y = PLAYER_VELOCITY * vector2.y;
        Mappers.state.get(player).set(GO_LEFT);
    }

    @Override
    public void goRightTop() {
        Vector2 vector2 = new Vector2(1f, 1f);
        vector2.nor();
        velocity.get(player).x = PLAYER_VELOCITY * vector2.x;
        velocity.get(player).y = PLAYER_VELOCITY * vector2.y;
        Mappers.state.get(player).set(GO_RIGHT);
    }

    @Override
    public void goRightBottom() {
        Vector2 vector2 = new Vector2(1f, -1f);
        vector2.nor();
        velocity.get(player).x = PLAYER_VELOCITY * vector2.x;
        velocity.get(player).y = PLAYER_VELOCITY * vector2.y;
        Mappers.state.get(player).set(GO_RIGHT);
    }

    @Override
    public void stop() {
        velocity.get(player).x = 0;
        velocity.get(player).y = 0;
        Mappers.state.get(player).set(ANIMATION_MAIN);
    }

}