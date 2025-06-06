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
package com.hippo.ehviewer.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.client.parser.GalleryPageUrlParser
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

private val intent = CustomTabsIntent.Builder().apply { setShowTitle(true) }.build()

fun Context.openBrowser(url: String) {
    if (url.isEmpty()) return
    try {
        intent.launchUrl(this, url.toUri())
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, R.string.no_browser_installed, Toast.LENGTH_LONG).show()
    }
}

fun DestinationsNavigator.jumpToReaderByPage(url: String, detail: GalleryDetail): Boolean {
    GalleryPageUrlParser.parse(url)?.let {
        if (it.gid == detail.gid) {
            navToReader(detail.galleryInfo, it.page)
            return true
        }
    }
    return false
}
