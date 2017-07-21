package com.benk97;

import android.os.Bundle;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.benk97.ads.AdsController;
import com.unity3d.ads.IUnityAdsListener;
import com.unity3d.ads.UnityAds;

public class AndroidLauncher extends AndroidApplication implements AdsController, IUnityAdsListener {

    private final String UNITY_ADS_GAME_ID = "1487325";
    private Runnable afterAd = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupAds();
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useImmersiveMode = true;
        config.numSamples = 2;
        initialize(new SpaceKillerGame(this), config);
    }

    public void setupAds() {
        UnityAds.initialize(this, UNITY_ADS_GAME_ID, this, SpaceKillerGameConstants.AD_TEST);
    }


    @Override
    public void showInterstitialAd(final Runnable then) {
        if (UnityAds.isReady()) {
            afterAd = then;
            UnityAds.show(this);
        } else {
            Gdx.app.postRunnable(then);
        }
    }


    @Override
    public void onUnityAdsReady(String s) {

    }

    @Override
    public void onUnityAdsStart(String s) {

    }

    @Override
    public void onUnityAdsFinish(String s, UnityAds.FinishState finishState) {
        if (afterAd != null) {
            Gdx.app.postRunnable(afterAd);
        }
    }

    @Override
    public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String s) {

    }
}
