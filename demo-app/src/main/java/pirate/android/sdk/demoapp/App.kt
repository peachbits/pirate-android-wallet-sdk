package pirate.android.sdk.demoapp

import androidx.multidex.MultiDexApplication
import pirate.android.sdk.internal.PirateTroubleshootingTwig
import pirate.android.sdk.internal.Twig

class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            StrictModeHelper.enableStrictMode()
        }

        Twig.plant(PirateTroubleshootingTwig())
    }
}
