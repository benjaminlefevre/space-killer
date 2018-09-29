/*
 * Developed by Benjamin Lefèvre
 * Last modified 29/09/18 22:06
 * Copyright (c) 2018. All rights reserved.
 */

package com.bendk97;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.bendk97.google.PlayServices;
import com.bendk97.screens.MenuScreen;

import java.io.File;

import static com.bendk97.SpaceKillerGameConstants.SKIP_SPLASH;

public class SpaceKillerGame extends Game {
    private com.bendk97.assets.Assets assets = new com.bendk97.assets.Assets();
    public PlayServices playServices;
    public Screen currentScreen;
    public com.bendk97.player.PlayerData playerData;
    public com.bendk97.share.IntentShare intentShare;

    public SpaceKillerGame(PlayServices playServices, com.bendk97.share.IntentShare intentShare) {
        this.playServices = playServices;
        this.intentShare = intentShare;
        if (com.bendk97.SpaceKillerGameConstants.DEBUG) {
            GLProfiler profiler = new GLProfiler(Gdx.graphics);
            profiler.enable();
        }
    }

    private void cleanTempDirectory() {
        if (Gdx.files.isExternalStorageAvailable()) {
            final File directory = Gdx.files.external(com.bendk97.screens.SocialScoreScreen.TEMP_DIRECTORY).file();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (File file : directory.listFiles()) {

                            file.delete();
                        }
                    } catch (Exception e) {
                    }
                }
            }).start();
        }
    }

    @Override
    public void create() {
        Gdx.input.setCatchBackKey(true);
        cleanTempDirectory();
        if (SKIP_SPLASH) {
            goToScreen(com.bendk97.screens.MenuScreen.class);
        } else {
            goToScreen(com.bendk97.screens.SplashScreen.class);
        }
    }

    public void goToScreen(Class screen) {
        goToScreen(screen, null, null);
    }

    public void goToScreen(Class screen, com.bendk97.player.PlayerData playerData, FrameBuffer screenshot) {
        try {
            assets.loadResources(screen);
            this.playerData = playerData;
            currentScreen = (Screen) screen.getConstructor(com.bendk97.assets.Assets.class, SpaceKillerGame.class).newInstance(assets, this);
            if (screenshot != null) {
                this.setScreen(new com.bendk97.screens.TransitionScreen(screenshot, currentScreen, this));
            } else {
                this.setScreen(currentScreen);
            }
        } catch (Exception e) {
            Gdx.app.log("Guru Meditation", "error: " + e.getMessage());
            Gdx.app.exit();
        } finally {
            Runtime.getRuntime().gc();
        }
    }


    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
    }

    public void signInSucceeded() {
        if (currentScreen instanceof com.bendk97.screens.MenuScreen) {
            signInFailed = false;
            ((com.bendk97.screens.MenuScreen) currentScreen).signInSucceeded();
        }
    }

    public boolean signInFailed = false;

    public void signInFailed() {
        if (currentScreen instanceof com.bendk97.screens.MenuScreen) {
            signInFailed = true;
            ((MenuScreen) currentScreen).signInFailed();
        }
    }

    public void continueWithExtraLife() {
        if (currentScreen instanceof com.bendk97.screens.LevelScreen) {
            ((com.bendk97.screens.LevelScreen) currentScreen).continueWithExtraLife();
        }
    }
}