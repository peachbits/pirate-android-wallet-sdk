package pirate.android.sdk.internal.db.block

import androidx.room.ColumnInfo
import androidx.room.Entity
import pirate.android.sdk.internal.model.CompactBlock
import pirate.android.sdk.model.BlockHeight
import pirate.android.sdk.model.FirstClassByteArray
import pirate.android.sdk.model.PirateNetwork

@Entity(primaryKeys = ["height"], tableName = "compactblocks")
data class PirateCompactBlockEntity(
    val height: Long,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PirateCompactBlockEntity) return false

        if (height != other.height) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = height.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }

    internal fun toCompactBlock(zcashNetwork: PirateNetwork) = CompactBlock(
        BlockHeight.new(zcashNetwork, height),
        FirstClassByteArray(data)
    )

    companion object {
        internal fun fromCompactBlock(compactBlock: CompactBlock) =
            PirateCompactBlockEntity(compactBlock.height.value, compactBlock.data.byteArray)
    }
}
