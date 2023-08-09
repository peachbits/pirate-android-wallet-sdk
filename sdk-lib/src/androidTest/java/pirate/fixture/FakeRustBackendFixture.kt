package pirate.fixture

import pirate.android.sdk.internal.model.JniBlockMeta
import pirate.android.sdk.model.PirateNetwork

internal class FakeRustBackendFixture {

    private val DEFAULT_NETWORK = PirateNetwork.Testnet

    fun new(
        network: PirateNetwork = DEFAULT_NETWORK,
        metadata: MutableList<JniBlockMeta> = mutableListOf()
    ) = FakeRustBackend(
        networkId = network.id,
        metadata = metadata
    )
}
