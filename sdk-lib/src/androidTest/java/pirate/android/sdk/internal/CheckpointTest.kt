package cash.z.ecc.android.sdk.internal

import androidx.test.filters.SmallTest
import pirate.android.sdk.internal.model.Checkpoint
import pirate.fixture.CheckpointFixture
import pirate.fixture.toJson
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class CheckpointTest {
    @Test
    @SmallTest
    fun deserialize() {
        val fixtureCheckpoint = CheckpointFixture.new()

        val deserialized = Checkpoint.from(CheckpointFixture.NETWORK, fixtureCheckpoint.toJson())

        assertEquals(fixtureCheckpoint, deserialized)
    }

    @Test
    @SmallTest
    fun epoch_seconds_as_long_that_would_overflow_int() {
        val jsonString = CheckpointFixture.new(time = Long.MAX_VALUE).toJson()

        Checkpoint.from(CheckpointFixture.NETWORK, jsonString).also {
            assertEquals(Long.MAX_VALUE, it.epochSeconds)
        }
    }

    @Test
    @SmallTest
    fun parse_height_as_long_that_would_overflow_int() {
        val jsonString = JSONObject().apply {
            put(Checkpoint.KEY_VERSION, Checkpoint.VERSION_1)
            put(Checkpoint.KEY_HEIGHT, UInt.MAX_VALUE.toLong())
            put(Checkpoint.KEY_HASH, CheckpointFixture.HASH)
            put(Checkpoint.KEY_EPOCH_SECONDS, CheckpointFixture.EPOCH_SECONDS)
            put(Checkpoint.KEY_TREE, CheckpointFixture.TREE)
        }.toString()

        Checkpoint.from(CheckpointFixture.NETWORK, jsonString).also {
            assertEquals(UInt.MAX_VALUE.toLong(), it.height.value)
        }
    }
}
