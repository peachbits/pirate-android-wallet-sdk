package pirate.android.sdk.demoapp

import android.app.Application
import pirate.android.sdk.internal.PirateTroubleshootingTwig
import pirate.android.sdk.internal.Twig

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            StrictModeHelper.enableStrictMode()
        }

        Twig.plant(PirateTroubleshootingTwig())
    }
}
