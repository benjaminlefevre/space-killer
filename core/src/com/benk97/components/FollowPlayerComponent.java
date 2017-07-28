package com.benk97.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.utils.Pool;

public class FollowPlayerComponent implements Component, Pool.Poolable{
    public float velocity = 0f;
    public float lastMove = 100f;
    @Override
    public void reset() {
        velocity = 0f;
        lastMove = 100f;
    }
}
