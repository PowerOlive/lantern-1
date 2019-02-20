/*
 * Copyright (C) 2017 Nishant Srivastava
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package github.nisrulz.lantern;

import static github.nisrulz.lantern.Utils.isMarshmallowAndAbove;

import android.Manifest;
import android.app.Activity;
import android.arch.lifecycle.Lifecycle.Event;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.OnLifecycleEvent;
import android.os.Handler;
import android.support.annotation.RequiresPermission;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

public class Lantern implements LifecycleObserver {

    private WeakReference<Activity> activityWeakRef;

    private final DisplayLightController displayLightController;

    private FlashController flashController;

    private final Handler handler;

    private boolean isFlashOn = false;

    private long pulseTime = 1000;

    private final Utils utils;

    private final Runnable pulseRunnable = new Runnable() {
        @Override
        public void run() {
            enableTorchMode(!isFlashOn);
            handler.postDelayed(pulseRunnable, pulseTime);
        }
    };


    public Lantern(Activity activity) {
        this.activityWeakRef = new WeakReference<>(activity);
        utils = new Utils();
        handler = new Handler();
        displayLightController = new DisplayLightControllerImpl(activity);
    }

    public Lantern alwaysOnDisplay(boolean enabled) {
        if (enabled) {
            displayLightController.enableAlwaysOnMode();
        } else {
            displayLightController.disableAlwaysOnMode();
        }
        return this;
    }

    public Lantern autoBright(boolean enabled) {
        if (enabled) {
            displayLightController.enableAutoBrightMode();
        } else {
            displayLightController.disableAutoBrightMode();
        }
        return this;
    }

    public Lantern checkAndRequestSystemPermission() {
        displayLightController.requestSystemWritePermission();
        return this;
    }

    @OnLifecycleEvent(Event.ON_DESTROY)
    public void cleanup() {
        handler.removeCallbacks(pulseRunnable);
        displayLightController.cleanup();
        this.activityWeakRef = null;
    }

    public Lantern enableTorchMode(boolean enabled) {
        if (activityWeakRef != null) {
            if (enabled) {
                if (!isFlashOn && utils.checkForCameraPermission(activityWeakRef.get().getApplicationContext())) {
                    flashController.on();
                    isFlashOn = true;
                }
            } else {
                if (isFlashOn && utils.checkForCameraPermission(activityWeakRef.get().getApplicationContext())) {
                    flashController.off();
                    isFlashOn = false;
                }
            }
        } else {
            flashController.off();
            isFlashOn = false;
        }
        return this;
    }

    public Lantern fullBrightDisplay(boolean enabled) {
        if (enabled) {
            displayLightController.enableFullBrightMode();
        } else {
            displayLightController.disableFullBrightMode();
        }
        return this;
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    public boolean initTorch() {
        if (activityWeakRef != null) {
            if (utils.checkIfCameraFeatureExists(activityWeakRef.get()) && utils
                    .checkForCameraPermission(activityWeakRef.get())) {
                if (isMarshmallowAndAbove()) {
                    flashController = new PostMarshmallow(activityWeakRef.get());
                } else {
                    flashController = new PreMarshmallow();
                }
                return true;
            }
        }
        return false;
    }

    public Lantern observeLifecycle(LifecycleOwner lifecycleOwner) {
        // Subscribe to listening lifecycle
        lifecycleOwner.getLifecycle().addObserver(this);

        return this;
    }

    public Lantern pulse(boolean enabled) {
        if (enabled) {
            handler.postDelayed(pulseRunnable, pulseTime);
        } else {
            handler.removeCallbacks(pulseRunnable);
        }
        return this;
    }

    public Lantern withDelay(long time, final TimeUnit timeUnit) {
        this.pulseTime = TimeUnit.MILLISECONDS.convert(time, timeUnit);
        return this;
    }
    
    public boolean isTorchOn() {
        return isFlashOn;
    }
}
