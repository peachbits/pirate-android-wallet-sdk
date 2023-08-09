package pirate.wallet.sdk.sample.demoapp

import androidx.test.platform.app.InstrumentationRegistry

import pirate.android.sdk.Synchronizer
import pirate.android.sdk.demoapp.util.fromResources
import pirate.android.sdk.ext.convertArrrToArrrtoshi
import pirate.android.sdk.ext.toHex
import pirate.android.sdk.internal.Twig
import pirate.android.sdk.model.Account
import pirate.android.sdk.model.BlockHeight
import pirate.android.sdk.model.Mainnet
import pirate.android.sdk.model.PirateNetwork
import pirate.android.sdk.model.defaultForNetwork
import pirate.android.sdk.tool.DerivationTool
import pirate.lightwallet.client.LightWalletClient
import pirate.lightwallet.client.model.BlockHeightUnsafe
import pirate.lightwallet.client.model.CompactBlockUnsafe
import pirate.lightwallet.client.model.LightWalletEndpoint
import pirate.lightwallet.client.model.Response
import pirate.lightwallet.client.new
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    // Get Address
    @Test
    fun getAddress() = runBlocking {
        val address = synchronizer.getUnifiedAddress(Account.DEFAULT)
        assertFalse(address.isBlank())
        log("Address: $address")
    }

    // ///////////////////////////////////////////////////
    // Derive address from Extended Full Viewing Key
    @Test
    fun getAddressFromViewingKey() {
    }

    // ///////////////////////////////////////////////////
    // Query latest block height
    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getLatestBlockHeightTest() = runTest {
        // Test the result, only if there is no server communication problem.
        runCatching {
            LightWalletClient.new(context, lightwalletdHost).getLatestBlockHeight()
        }.onFailure {
            Twig.debug(it) { "Failed to retrieve data" }
        }.onSuccess {
            assertTrue(it is Response.Success<BlockHeightUnsafe>)
            Twig.debug { "Latest Block: ${(it as Response.Success<BlockHeightUnsafe>).result}" }
        }
    }

    // ///////////////////////////////////////////////////
    // Download compact block range
    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getBlockRange() = runTest {
        val blockRange = BlockHeightUnsafe(
            BlockHeight.new(
                PirateNetwork.Mainnet,
                500_000
            ).value
        )..BlockHeightUnsafe(
            (
                BlockHeight.new(
                    PirateNetwork.Mainnet,
                    500_009
                ).value
                )
        )

        val lightWalletClient = LightWalletClient.new(context, lightwalletdHost)

        // Test the result, only if there is no server communication problem.
        runCatching {
            lightWalletClient.getBlockRange(blockRange)
        }.onFailure {
            Twig.debug(it) { "Failed to retrieve data" }
        }.onSuccess {
            it.onEach { response ->
                assert(response is Response.Success) { "Server communication failed." }
            }
                .filterIsInstance<Response.Success<CompactBlockUnsafe>>()
                .map { response ->
                    response.result
                }.toList()
                .also { blocks ->
                    assertEquals(blockRange.endInclusive.value - blockRange.start.value, blocks.count())

                    blocks.forEachIndexed { i, block ->
                        log("Block #$i:    height:${block.height}   hash:${block.hash.toHex()}")
                    }
                }
        }
    }

    // ///////////////////////////////////////////////////
    // Query account outgoing transactions
    @Test
    fun queryOutgoingTransactions() {
    }

    // ///////////////////////////////////////////////////
    // Query account incoming transactions
    @Test
    fun queryIncomingTransactions() {
    }

//    // ///////////////////////////////////////////////////
//    // Create a signed transaction (with memo)
//    @Test fun createTransaction() = runBlocking {
//        val rustBackend = RustBackend.init(context)
//        val repository = PagedTransactionRepository(context)
//        val encoder = PirateWalletTransactionEncoder(rustBackend, repository)
//        val spendingKey = DerivationTool.deriveSpendingKeys(seed, PirateNetwork.Mainnet)[0]
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
    @Test
    fun submitTransaction() = runBlocking {
        val amount = 0.123.convertArrrToArrrtoshi()
        val address = "ztestsapling1tklsjr0wyw0d58f3p7wufvrj2cyfv6q6caumyueadq8qvqt8lda6v6tpx474rfru9y6u75u7qnw"
        val memo = "Test Transaction"
        val spendingKey = DerivationTool.getInstance().deriveUnifiedSpendingKey(
            seed,
            PirateNetwork.Mainnet,
            Account.DEFAULT
        )
        synchronizer.sendToAddress(spendingKey, amount, address, memo)
    }

    // /////////////////////////////////////////////////////
    // Utility Functions
    // ////////////////////////////////////////////////////

    companion object {
        private val seed = "Insert seed for testing".toByteArray()
        private val lightwalletdHost = PirateLightWalletEndpoint.Mainnet

        private val context = InstrumentationRegistry.getInstrumentation().targetContext
        private val synchronizer: Synchronizer = run {
            val network = PirateNetwork.fromResources(context)
            Synchronizer.newBlocking(
                context,
                network,
                lightWalletEndpoint = LightWalletEndpoint.defaultForNetwork(network),
                seed = seed,
                birthday = null
            )
        }

        fun log(message: String?) = Twig.debug { message ?: "null" }
    }
}
