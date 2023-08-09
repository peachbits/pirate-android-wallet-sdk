package pirate.android.sdk.internal

import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import pirate.android.bip39.Mnemonics
import pirate.android.sdk.Synchronizer
import pirate.android.sdk.fixture.WalletFixture
import pirate.android.sdk.model.PirateNetwork
import pirate.android.sdk.model.defaultForNetwork
import pirate.lightwallet.client.model.LightWalletEndpoint
import kotlinx.coroutines.test.runTest
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFailsWith

class SdkSynchronizerTest {

    @Test
    @SmallTest
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun cannot_instantiate_in_parallel() = runTest {
        // Random alias so that repeated invocations of this test will have a clean starting state
        val alias = UUID.randomUUID().toString()

        // In the future, inject fake networking component so that it doesn't require hitting the network
        Synchronizer.new(
            InstrumentationRegistry.getInstrumentation().context,
            PirateNetwork.Mainnet,
            alias,
            LightWalletEndpoint.defaultForNetwork(PirateNetwork.Mainnet),
            Mnemonics.MnemonicCode(WalletFixture.SEED_PHRASE).toEntropy(),
            birthday = null
        ).use {
            assertFailsWith<IllegalStateException> {
                Synchronizer.new(
                    InstrumentationRegistry.getInstrumentation().context,
                    PirateNetwork.Mainnet,
                    alias,
                    LightWalletEndpoint.defaultForNetwork(PirateNetwork.Mainnet),
                    Mnemonics.MnemonicCode(WalletFixture.SEED_PHRASE).toEntropy(),
                    birthday = null
                )
            }
        }
    }

    @Test
    @SmallTest
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun can_instantiate_in_serial() = runTest {
        // Random alias so that repeated invocations of this test will have a clean starting state
        val alias = UUID.randomUUID().toString()

        // In the future, inject fake networking component so that it doesn't require hitting the network
        Synchronizer.new(
            InstrumentationRegistry.getInstrumentation().context,
            PirateNetwork.Mainnet,
            alias,
            LightWalletEndpoint.defaultForNetwork(PirateNetwork.Mainnet),
            Mnemonics.MnemonicCode(WalletFixture.SEED_PHRASE).toEntropy(),
            birthday = null
        ).use {}

        // Second instance should succeed because first one was closed
        Synchronizer.new(
            InstrumentationRegistry.getInstrumentation().context,
            PirateNetwork.Mainnet,
            alias,
            LightWalletEndpoint.defaultForNetwork(PirateNetwork.Mainnet),
            Mnemonics.MnemonicCode(WalletFixture.SEED_PHRASE).toEntropy(),
            birthday = null
        ).use {}
    }
}
