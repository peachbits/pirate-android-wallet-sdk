package pirate.android.sdk.util

import pirate.android.sdk.internal.deriveUnifiedAddress
import pirate.android.sdk.internal.jni.RustDerivationTool
import pirate.android.sdk.model.Account
import pirate.android.sdk.model.PirateNetwork
import pirate.android.sdk.test.readFileLinesInFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@ExperimentalCoroutinesApi
class AddressGeneratorUtil {

    private val mnemonics = SimpleMnemonics()

    @Test
    fun printMnemonic() {
        mnemonics.apply {
            val mnemonicPhrase = String(nextMnemonic())
            println("example mnemonic: $mnemonicPhrase")
            assertEquals(24, mnemonicPhrase.split(" ").size)
        }
    }

    @Test
    fun generateAddresses() = runBlocking {
        readFileLinesInFlow("/utils/seeds.txt")
            .map { seedPhrase ->
                mnemonics.toSeed(seedPhrase.toCharArray())
            }.map { seed ->
                RustDerivationTool.new().deriveUnifiedAddress(seed, PirateNetwork.Mainnet, Account.DEFAULT)
            }.collect { address ->
                println("xrxrx2\t$address")
                assertTrue(address.startsWith("u1"))
            }
    }
}
