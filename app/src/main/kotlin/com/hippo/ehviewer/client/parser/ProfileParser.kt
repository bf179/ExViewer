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
package com.hippo.ehviewer.client.parser

import arrow.core.Either
import arrow.core.getOrElse
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.exception.ParseException
import com.hippo.ehviewer.client.parser.SignInParser.ERROR_PATTERN
import eu.kanade.tachiyomi.util.system.logcat
import org.jsoup.Jsoup

object ProfileParser {
    fun parse(body: String): Result = Either.catch {
        val d = Jsoup.parse(body)
        val profilename = d.getElementById("profilename")
        val displayName = profilename!!.child(0).text()
        val avatar = Either.catch {
            val avatar =
                profilename.nextElementSibling()!!.nextElementSibling()!!.child(0).attr("src")
            if (avatar.isEmpty()) {
                null
            } else if (!avatar.startsWith("http")) {
                EhUrl.URL_FORUMS + avatar
            } else {
                avatar
            }
        }.getOrElse {
            logcat { "No avatar" }
            null
        }
        Result(displayName, avatar)
    }.getOrElse {
        ERROR_PATTERN.find(body)?.let {
            val displayName = Jsoup.parse(body).select("p.home > b > a").first()?.text()
            logcat { it.groupValues[1].ifEmpty { it.groupValues[2] } }
            Result(displayName, null)
        } ?: throw ParseException("Parse forums error")
    }

    class Result(val displayName: String?, val avatar: String?)
}
