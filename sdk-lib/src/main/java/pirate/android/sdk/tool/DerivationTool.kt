package pirate.android.sdk.tool

import pirate.android.sdk.jni.PirateRustBackend
import pirate.android.sdk.jni.PirateRustBackendWelding
import pirate.android.sdk.type.UnifiedViewingKey
import pirate.android.sdk.type.PirateNetwork

class DerivationTool {

    companion object : PirateRustBackendWelding.Derivation {

        /**
         * Given a seed and a number of accounts, return the associated viewing keys.
         *
         * @param seed the seed from which to derive viewing keys.
         * @param numberOfAccounts the number of accounts to use. Multiple accounts are not fully
         * supported so the default value of 1 is recommended.
         *
         * @return the viewing keys that correspond to the seed, formatted as Strings.
         */
        override suspend fun deriveUnifiedViewingKeys(seed: ByteArray, network: PirateNetwork, numberOfAccounts: Int): Array<UnifiedViewingKey> =
            withPirateRustBackendLoaded {
                deriveUnifiedViewingKeysFromSeed(seed, numberOfAccounts, networkId = network.id).map {
                    UnifiedViewingKey(it[0], it[1])
                }.toTypedArray()
            }

        /**
         * Given a spending key, return the associated viewing key.
         *
         * @param spendingKey the key from which to derive the viewing key.
         *
         * @return the viewing key that corresponds to the spending key.
         */
        override suspend fun deriveViewingKey(spendingKey: String, network: PirateNetwork): String = withPirateRustBackendLoaded {
            deriveExtendedFullViewingKey(spendingKey, networkId = network.id)
        }

        /**
         * Given a seed and a number of accounts, return the associated spending keys.
         *
         * @param seed the seed from which to derive spending keys.
         * @param numberOfAccounts the number of accounts to use. Multiple accounts are not fully
         * supported so the default value of 1 is recommended.
         *
         * @return the spending keys that correspond to the seed, formatted as Strings.
         */
        override suspend fun deriveSpendingKeys(seed: ByteArray, network: PirateNetwork, numberOfAccounts: Int): Array<String> =
            withPirateRustBackendLoaded {
                deriveExtendedSpendingKeys(seed, numberOfAccounts, networkId = network.id)
            }

        /**
         * Given a seed and account index, return the associated address.
         *
         * @param seed the seed from which to derive the address.
         * @param accountIndex the index of the account to use for deriving the address. Multiple
         * accounts are not fully supported so the default value of 1 is recommended.
         *
         * @return the address that corresponds to the seed and account index.
         */
        override suspend fun deriveShieldedAddress(seed: ByteArray, network: PirateNetwork, accountIndex: Int): String =
            withPirateRustBackendLoaded {
                deriveShieldedAddressFromSeed(seed, accountIndex, networkId = network.id)
            }

        /**
         * Given a viewing key string, return the associated address.
         *
         * @param viewingKey the viewing key to use for deriving the address. The viewing key is tied to
         * a specific account so no account index is required.
         *
         * @return the address that corresponds to the viewing key.
         */
        override suspend fun deriveShieldedAddress(viewingKey: String, network: PirateNetwork): String = withPirateRustBackendLoaded {
            deriveShieldedAddressFromViewingKey(viewingKey, networkId = network.id)
        }

        // WIP probably shouldn't be used just yet. Why?
        //  - because we need the private key associated with this seed and this function doesn't return it.
        //  - the underlying implementation needs to be split out into a few lower-level calls
        override suspend fun deriveTransparentAddress(seed: ByteArray, network: PirateNetwork, account: Int, index: Int): String = withPirateRustBackendLoaded {
            deriveTransparentAddressFromSeed(seed, account, index, networkId = network.id)
        }

        override suspend fun deriveTransparentAddressFromPublicKey(transparentPublicKey: String, network: PirateNetwork): String = withPirateRustBackendLoaded {
            deriveTransparentAddressFromPubKey(transparentPublicKey, networkId = network.id)
        }

        override suspend fun deriveTransparentAddressFromPrivateKey(transparentPrivateKey: String, network: PirateNetwork): String = withPirateRustBackendLoaded {
            deriveTransparentAddressFromPrivKey(transparentPrivateKey, networkId = network.id)
        }

        override suspend fun deriveTransparentSecretKey(seed: ByteArray, network: PirateNetwork, account: Int, index: Int): String = withPirateRustBackendLoaded {
            deriveTransparentSecretKeyFromSeed(seed, account, index, networkId = network.id)
        }

        fun validateUnifiedViewingKey(viewingKey: UnifiedViewingKey, networkId: Int = PirateNetwork.Mainnet.id) {
            // TODO
        }

        /**
         * A helper function to ensure that the Rust libraries are loaded before any code in this
         * class attempts to interact with it, indirectly, by invoking JNI functions. It would be
         * nice to have an annotation like @UsesSystemLibrary for this
         */
        private suspend fun <T> withPirateRustBackendLoaded(block: () -> T): T {
            PirateRustBackend.rustLibraryLoader.load()
            return block()
        }

        //
        // JNI functions
        //

        @JvmStatic
        private external fun deriveExtendedSpendingKeys(
            seed: ByteArray,
            numberOfAccounts: Int,
            networkId: Int,
        ): Array<String>

        @JvmStatic
        private external fun deriveUnifiedViewingKeysFromSeed(
            seed: ByteArray,
            numberOfAccounts: Int,
            networkId: Int,
        ): Array<Array<String>>

        @JvmStatic
        private external fun deriveExtendedFullViewingKey(spendingKey: String, networkId: Int): String

        @JvmStatic
        private external fun deriveShieldedAddressFromSeed(
            seed: ByteArray,
            accountIndex: Int,
            networkId: Int,
        ): String

        @JvmStatic
        private external fun deriveShieldedAddressFromViewingKey(key: String, networkId: Int): String

        @JvmStatic
        private external fun deriveTransparentAddressFromSeed(seed: ByteArray, account: Int, index: Int, networkId: Int): String

        @JvmStatic
        private external fun deriveTransparentAddressFromPubKey(pk: String, networkId: Int): String

        @JvmStatic
        private external fun deriveTransparentAddressFromPrivKey(sk: String, networkId: Int): String

        @JvmStatic
        private external fun deriveTransparentSecretKeyFromSeed(seed: ByteArray, account: Int, index: Int, networkId: Int): String
    }
}
