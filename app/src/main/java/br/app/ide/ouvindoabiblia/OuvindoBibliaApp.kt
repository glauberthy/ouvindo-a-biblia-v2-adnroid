package br.app.ide.ouvindoabiblia

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.StrictMode
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class OuvindoBibliaApp : Application(), ImageLoaderFactory {

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun onCreate() {
        super.onCreate()
        if (isDebuggable()) enableStrictMode()
    }

    override fun newImageLoader(): ImageLoader {
        return imageLoader
    }

    private fun isDebuggable(): Boolean =
        (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    /**
     * StrictMode só em builds debug (ISSUE 4.C): loga (não mata) violações de I/O de
     * disco/rede na main thread e vazamentos de Activity/Closable/SQLite — ajuda a
     * flagrar regressões cedo sem impactar release.
     */
    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectActivityLeaks()
                .detectLeakedClosableObjects()
                .detectLeakedSqlLiteObjects()
                .penaltyLog()
                .build()
        )
    }
}
