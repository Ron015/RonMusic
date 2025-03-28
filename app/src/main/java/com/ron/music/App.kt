package com.ron.music

import android.app.Application
import android.os.Build
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.datastore.preferences.core.edit
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import com.ron.innertube.YouTube
import com.ron.innertube.models.YouTubeLocale
import com.ron.innertube.utils.parseCookieString
import com.ron.music.constants.ContentCountryKey
import com.ron.music.constants.ContentLanguageKey
import com.ron.music.constants.CountryCodeToName
import com.ron.music.constants.InnerTubeCookieKey
import com.ron.music.constants.LanguageCodeToName
import com.ron.music.constants.MaxImageCacheSizeKey
import com.ron.music.constants.ProxyEnabledKey
import com.ron.music.constants.ProxyTypeKey
import com.ron.music.constants.ProxyUrlKey
import com.ron.music.constants.SYSTEM_DEFAULT
import com.ron.music.constants.UseLoginForBrowse
import com.ron.music.constants.VisitorDataKey
import com.ron.music.extensions.toEnum
import com.ron.music.extensions.toInetSocketAddress
import com.ron.music.utils.dataStore
import com.ron.music.utils.get
import com.ron.music.utils.reportException
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.Proxy
import java.util.Locale

@HiltAndroidApp
class App :
    Application(),
    ImageLoaderFactory {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())

        val locale = Locale.getDefault()
        val languageTag = locale.toLanguageTag().replace("-Hant", "") // replace zh-Hant-* to zh-*
        YouTube.locale =
            YouTubeLocale(
                gl =
                dataStore[ContentCountryKey]?.takeIf { it != SYSTEM_DEFAULT }
                    ?: locale.country.takeIf { it in CountryCodeToName }
                    ?: "US",
                hl =
                dataStore[ContentLanguageKey]?.takeIf { it != SYSTEM_DEFAULT }
                    ?: locale.language.takeIf { it in LanguageCodeToName }
                    ?: languageTag.takeIf { it in LanguageCodeToName }
                    ?: "en",
            )

        if (dataStore[ProxyEnabledKey] == true) {
            try {
                YouTube.proxy =
                    Proxy(
                        dataStore[ProxyTypeKey].toEnum(defaultValue = Proxy.Type.HTTP),
                        dataStore[ProxyUrlKey]!!.toInetSocketAddress(),
                    )
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to parse proxy url.", LENGTH_SHORT).show()
                reportException(e)
            }
        }

        if (dataStore[UseLoginForBrowse] == true) {
            YouTube.useLoginForBrowse = true
        }

        GlobalScope.launch {
            dataStore.data
                .map { it[VisitorDataKey] }
                .distinctUntilChanged()
                .collect { visitorData ->
                    YouTube.visitorData = visitorData
                        ?.takeIf { it != "null" } // Previously visitorData was sometimes saved as "null" due to a bug
                        ?: YouTube.visitorData().getOrNull()?.also { newVisitorData ->
                            dataStore.edit { settings ->
                                settings[VisitorDataKey] = newVisitorData
                            }
                        } ?: YouTube.DEFAULT_VISITOR_DATA
                }
        }
        GlobalScope.launch {
            dataStore.data
                .map { it[InnerTubeCookieKey] }
                .distinctUntilChanged()
                .collect { cookie ->
                    try {
                        YouTube.cookie =
                            if ("SAPISID" in parseCookieString(cookie ?: "")) cookie else null
                    } catch (e: Exception) {
                        Timber.e("Could not parse cookie. Clearing existing cookie. %s", e.message)
                        dataStore.edit { settings ->
                            settings[InnerTubeCookieKey] = ""
                        }
                    }
                }
        }
    }

    override fun newImageLoader() =
        ImageLoader
            .Builder(this)
            .crossfade(true)
            .respectCacheHeaders(false)
            .allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            .diskCache(
                DiskCache
                    .Builder()
                    .directory(cacheDir.resolve("coil"))
                    .maxSizeBytes((dataStore[MaxImageCacheSizeKey] ?: 512) * 1024 * 1024L)
                    .build(),
            ).build()
}
    
