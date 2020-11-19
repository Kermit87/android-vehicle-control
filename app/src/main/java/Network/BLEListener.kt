package Network

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.ScanResult

interface BLEListener {
    // Interface that must implement if to inform from the service
    fun connected(gatt: BluetoothGatt?)
    fun disconnected()
    fun writeCharacteristicSuccessfully()
    fun scanResult(callbackType: Int, result: ScanResult?)
    fun serviceDiscoveredResult(gatt: BluetoothGatt?, status: Int)
    fun characteristicChanged(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?
    )
    fun scanTimeout()
}