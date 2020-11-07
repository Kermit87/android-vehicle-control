package Data

import Enums.Direction
import Enums.Orientation
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


@Parcelize
data class VehicleMotion(var orientation: Orientation,
                         var direction : Direction,
                         var speed: Int): Parcelable {
}