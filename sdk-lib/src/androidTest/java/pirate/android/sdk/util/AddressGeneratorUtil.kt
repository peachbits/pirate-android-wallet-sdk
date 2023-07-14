package pirate.android.sdk.util

import pirate.android.sdk.model.PirateNetwork
import cash.z.ecc.android.sdk.test.readFileLinesInFlow
import pirate.android.sdk.tool.PirateDerivationTool
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
                PirateDerivationTool.deriveShieldedAddress(seed, PirateNetwork.Mainnet)
            }.collect { address ->
                println("xrxrx2\t$address")
                assertTrue(address.startsWith("zs1"))
            }
    }
}
