/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.reveal;

import android.animation.Animator;
import android.view.View;

public final class ViewAnimationUtils {
    // http://developer.android.com/guide/topics/graphics/hardware-accel.html#unsupported

    public static Animator createCircularReveal(View view,
                                                int centerX, int centerY, float startRadius, float endRadius) {
        return android.view.ViewAnimationUtils.createCircularReveal(
                view, centerX, centerY, startRadius, endRadius);
    }
}
