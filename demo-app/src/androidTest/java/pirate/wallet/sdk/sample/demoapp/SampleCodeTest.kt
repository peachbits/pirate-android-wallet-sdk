package pirate.wallet.sdk.sample.demoapp

import androidx.test.platform.app.InstrumentationRegistry
import pirate.android.sdk.PirateInitializer
import pirate.android.sdk.Synchronizer
import pirate.android.sdk.db.entity.isFailure
import pirate.android.sdk.ext.convertArrrToArrrtoshi
import pirate.android.sdk.ext.toHex
import pirate.android.sdk.internal.PirateTroubleshootingTwig
import pirate.android.sdk.internal.Twig
import pirate.android.sdk.internal.service.PirateLightWalletGrpcService
import pirate.android.sdk.internal.twig
import pirate.android.sdk.model.BlockHeight
import pirate.android.sdk.tool.PirateDerivationTool
import pirate.android.sdk.type.PirateNetwork
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test

/**
 * Sample code to demonstrate key functionality without UI, inspired by:
 * https://github.com/EdgeApp/eosjs-node-cli/blob/paul/cleanup/app.js
 */
class SampleCodeTest {

    // ///////////////////////////////////////////////////
    // Seed derivation
    @Ignore("This test is not implemented")
    @Test
    fun createBip39Seed_fromSeedPhrase() {
        // TODO: log(seedPhrase.asRawEntropy().asBip39seed())
    }

    @Ignore("This test is not implemented")
    @Test
    fun createRawEntropy() {
        // TODO: call: Mnemonic::from_phrase(seed_phrase, Language::English).unwrap().entropy()
        // log(seedPhrase.asRawEntropy())
    }

    @Ignore("This test is not implemented")
    @Test
    fun createBip39Seed_fromRawEntropy() {
        // get the 64 byte bip39 entropy
        // TODO: call: bip39::Seed::new(&Mnemonic::from_entropy(&seed_bytes, Language::English).unwrap(), "")
        // log(rawEntropy.asBip39Seed())
    }

    @Ignore("This test is not implemented")
    @Test
    fun deriveSeedPhraseFrom() {
        // TODO: let mnemonic = Mnemonic::from_entropy(entropy, Language::English).unwrap();
        // log(entropy.asSeedPhrase())
    }

    // ///////////////////////////////////////////////////
    // Derive Extended Spending Key
    @Test fun deriveSpendingKey() {
        val spendingKeys = runBlocking {
            PirateDerivationTool.deriveSpendingKeys(
                seed,
                PirateNetwork.Mainnet
            )
        }
        assertEquals(1, spendingKeys.size)
        log("Spending Key: ${spendingKeys?.get(0)}")
    }

    // ///////////////////////////////////////////////////
    // Get Address
    @Test fun getAddress() = runBlocking {
        val address = synchronizer.getAddress()
        assertFalse(address.isNullOrBlank())
        log("Address: $address")
    }

    // ///////////////////////////////////////////////////
    // Derive address from Extended Full Viewing Key
    @Test fun getAddressFromViewingKey() {
    }

    // ///////////////////////////////////////////////////
    // Query latest block height
    @Test fun getLatestBlockHeightTest() {
        val lightwalletService = PirateLightWalletGrpcService(context, lightwalletdHost)
        log("Latest Block: ${lightwalletService.getLatestBlockHeight()}")
    }

    // ///////////////////////////////////////////////////
    // Download compact block range
    @Test fun getBlockRange() {
        val blockRange = BlockHeight.new(PirateNetwork.Mainnet, 500_000)..BlockHeight.new(PirateNetwork.Mainnet, 500_009)
        val lightwalletService = PirateLightWalletGrpcService(context, lightwalletdHost)
        val blocks = lightwalletService.getBlockRange(blockRange)
        assertEquals(blockRange.endInclusive.value - blockRange.start.value, blocks.count())

        blocks.forEachIndexed { i, block ->
            log("Block #$i:    height:${block.height}   hash:${block.hash.toByteArray().toHex()}")
        }
    }

    // ///////////////////////////////////////////////////
    // Query account outgoing transactions
    @Test fun queryOutgoingTransactions() {
    }

    // ///////////////////////////////////////////////////
    // Query account incoming transactions
    @Test fun queryIncomingTransactions() {
    }

//    // ///////////////////////////////////////////////////
//    // Create a signed transaction (with memo)
//    @Test fun createTransaction() = runBlocking {
//        val rustBackend = PirateRustBackend.init(context)
//        val repository = PiratePagedTransactionRepository(context)
//        val encoder = PirateWalletTransactionEncoder(rustBackend, repository)
//        val spendingKey = PirateDerivationTool.deriveSpendingKeys(seed, PirateNetwork.Mainnet)[0]
//
//        val amount = 0.123.convertArrrToArrrtoshi()
//        val address = "ztestsapling1tklsjr0wyw0d58f3p7wufvrj2cyfv6q6caumyueadq8qvqt8lda6v6tpx474rfru9y6u75u7qnw"
//        val memo = "Test Transaction".toByteArray()
//
//        val encodedTx = encoder.createTransaction(spendingKey, amount, address, memo)
//        assertTrue(encodedTx.raw.isNotEmpty())
//        log("Transaction ID: ${encodedTx.txId.toHex()}")
//    }

    // ///////////////////////////////////////////////////
    // Create a signed transaction (with memo) and broadcast
    @Test fun submitTransaction() = runBlocking {
        val amount = 0.123.convertArrrToArrrtoshi()
        val address = "ztestsapling1tklsjr0wyw0d58f3p7wufvrj2cyfv6q6caumyueadq8qvqt8lda6v6tpx474rfru9y6u75u7qnw"
        val memo = "Test Transaction"
        val spendingKey = PirateDerivationTool.deriveSpendingKeys(seed, PirateNetwork.Mainnet)[0]
        val transactionFlow = synchronizer.sendToAddress(spendingKey, amount, address, memo)
        transactionFlow.collect {
            log("pending transaction updated $it")
            assertTrue("Failed to send funds. See log for details.", !it.isFailure())
        }
    }

    // /////////////////////////////////////////////////////
    // Utility Functions
    // ////////////////////////////////////////////////////

    companion object {
        private val seed = "Insert seed for testing".toByteArray()
        private val lightwalletdHost: String = PirateNetwork.Mainnet.defaultHost

        private val context = InstrumentationRegistry.getInstrumentation().targetContext
        private val synchronizer: Synchronizer = run {
            val initializer = runBlocking { PirateInitializer.new(context) {} }
            Synchronizer.newBlocking(initializer)
        }

        @BeforeClass
        @JvmStatic
        fun init() {
            Twig.plant(PirateTroubleshootingTwig())
        }

        fun log(message: String?) = twig(message ?: "null")
    }
}
