package pirate.android.sdk.demoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pirate.android.sdk.demoapp.ui.common.BindCompLocalProvider
import pirate.android.sdk.demoapp.ui.screen.home.viewmodel.SecretState
import pirate.android.sdk.demoapp.ui.screen.home.viewmodel.WalletViewModel
import pirate.android.sdk.demoapp.ui.screen.seed.view.Seed
import pirate.android.sdk.demoapp.util.fromResources
import pirate.android.sdk.model.PirateNetwork

class ComposeActivity : ComponentActivity() {
    private val walletViewModel by viewModels<WalletViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BindCompLocalProvider {
                MaterialTheme {
                    Surface {
                        MainContent()
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalLifecycleComposeApi::class)
    @Composable
    private fun MainContent() {
        when (walletViewModel.secretState.collectAsStateWithLifecycle().value) {
            SecretState.Loading -> {
                // In the future, we might consider displaying something different here.
            }
            SecretState.None -> {
                Seed(
                    PirateNetwork.fromResources(applicationContext),
                    onExistingWallet = { walletViewModel.persistExistingWallet(it) },
                    onNewWallet = { walletViewModel.persistNewWallet() }
                )
            }
            is SecretState.Ready -> {
                Navigation()
            }
        }
    }
}
