package pirate.android.sdk.demoapp.model

import pirate.android.sdk.PirateSynchronizer
import pirate.android.sdk.model.Account

data class WalletAddresses(
    val unified: WalletAddress.Unified,
    val sapling: WalletAddress.Sapling,
    val transparent: WalletAddress.Transparent
) {
    // Override to prevent leaking details in logs
    override fun toString() = "WalletAddresses"

    companion object {
        suspend fun new(synchronizer: PirateSynchronizer): WalletAddresses {
            val unified = WalletAddress.Unified.new(
                synchronizer.getUnifiedAddress(Account.DEFAULT)
            )

            val saplingAddress = WalletAddress.Sapling.new(
                synchronizer.getSaplingAddress(Account.DEFAULT)
            )

            val transparentAddress = WalletAddress.Transparent.new(
                synchronizer.getTransparentAddress(Account.DEFAULT)
            )

            return WalletAddresses(
                unified = unified,
                sapling = saplingAddress,
                transparent = transparentAddress
            )
        }
    }
}
