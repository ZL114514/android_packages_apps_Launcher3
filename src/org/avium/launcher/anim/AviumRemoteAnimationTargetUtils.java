/*
 * Copyright (C) 2026 The AviumUI Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.avium.launcher.anim;

import android.app.ActivityManager;
import android.content.ComponentName;

import androidx.annotation.Nullable;

public final class AviumRemoteAnimationTargetUtils {

    @Nullable
    public static String getPackageName(@Nullable Object runningTaskTarget) {
        if (runningTaskTarget == null) {
            return null;
        }
        final ActivityManager.RunningTaskInfo taskInfo;
        try {
            java.lang.reflect.Field f = runningTaskTarget.getClass().getDeclaredField("taskInfo");
            f.setAccessible(true);
            taskInfo = (ActivityManager.RunningTaskInfo) f.get(runningTaskTarget);
        } catch (Exception e) {
            return null;
        }
        if (taskInfo == null) {
            return null;
        }

        final ComponentName[] taskInfoActivities = new ComponentName[]{
                taskInfo.baseActivity,
                taskInfo.origActivity,
                taskInfo.realActivity,
                taskInfo.topActivity};

        for (ComponentName component : taskInfoActivities) {
            if (component != null && component.getPackageName() != null) {
                return component.getPackageName();
            }
        }
        return null;
    }

    private AviumRemoteAnimationTargetUtils() {
    }
}
