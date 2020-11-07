package Network

import android.app.Service
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import java.util.*

class BLEService(val context: Context): Service() {

    private val uuidService = ParcelUuid.fromString("19B10000-E8F2-537E-4F6C-D104768A1214") // UUID for the service
    private val uuidControl = UUID.fromString("19B10001-E8F2-537E-4F6C-D104768A1214") // UUID for control characteristic
    private val uuidStateNotify = UUID.fromString("19B10012-E8F2-537E-4F6C-D104768A1214") // UUID for notify changes characteristic
    private val cccDescriptorUuid = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

    private var bluetoothGatt: BluetoothGatt? = null
    private var listener: BLEListener? = null   // instance which will be notify
    private var mScanning: Boolean = false
    private var binder: IBinder? = null

    private val bluetoothAdapter: BluetoothAdapter by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    inner class BLEBinder : Binder() {
        fun getService(): BLEService = this@BLEService
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    init {
        binder = BLEBinder()
    }
    // to set the instance which will be notify
    fun attach(listener: BLEListener){
        this.listener = listener
    }

    fun scanLeDevice(enable: Boolean) {
        // init the filter so that only searching for our service
        val filterList = ArrayList<ScanFilter>()
        val filter = ScanFilter.Builder().setServiceUuid(uuidService).build()
        filterList.add(filter)
        // set the type of scan-mode
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        when (enable) {
            true -> {
                // Stops scanning after a pre-defined scan period.
                Handler().postDelayed({
                    mScanning = false
                    stopBLEScan()
                }, 5000)
                mScanning = true
                startBLEScan(filterList, scanSettings)
            }
            else -> {
                mScanning = false
                stopBLEScan()
            }
        }
    }

    private fun stopBLEScan(){
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(mLeScanCallback)
    }

    private fun startBLEScan(filterList: ArrayList<ScanFilter>, scanSettings: ScanSettings){
        bluetoothAdapter?.bluetoothLeScanner?.startScan(
            filterList as List<ScanFilter>,
            scanSettings,
            mLeScanCallback
        )
    }

    fun connectToGatt(result: ScanResult){
        result.device.connectGatt(this, false, gattCallback)
    }

    fun disconnectToGatt(){
        bluetoothGatt?.disconnect()
    }

    fun discoverServices(gatt: BluetoothGatt?){
        gatt?.discoverServices()
    }
    // send the data to control the vehicle
    fun writeStringCharacterToBLEService(msg: String){
        val characteristic =  bluetoothGatt?.findCharacteristic(uuidControl)
        if (characteristic == null){
            Log.i("findCharacteristic", "\nCharacteristics ${uuidControl} not found")
            return
        }
        writeCharacteristic(characteristic, msg.toByteArray())
    }

    // Defined Callbacks for scanning and gatt connection result
    private var mLeScanCallback: ScanCallback =
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)
                if (result != null) {
                    listener?.scanResult(callbackType, result)
                }
            }

            override fun onBatchScanResults(results: List<ScanResult?>?) {
                super.onBatchScanResults(results)
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
            }
        }
    // BluetoothGatt- callback methods
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully connected to $deviceAddress")
                    bluetoothGatt = gatt
                    listener?.connected(gatt)
                    //gatt.discoverServices()

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully disconnected from $deviceAddress")
                    gatt.close()
                    listener?.disconnected()
                }
            } else {
                Log.w(
                    "BluetoothGattCallback",
                    "Error $status encountered for $deviceAddress! Disconnecting..."
                )
                gatt.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            listener?.serviceDiscoveredResult(gatt, status)
            var str = ""
            var chars = ""
            gatt?.services?.forEach{ service ->
                service.characteristics.forEach{ char ->
                    chars += "${char.properties} || "
                }
                str += "UUID: ${service.uuid} :: Cha: \n$chars\n"
            }

            val characteristic = bluetoothGatt?.findCharacteristic(uuidStateNotify)
            if (characteristic != null){
                //enableNotifications(characteristic)
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        listener?.writeCharacteristicSuccessfully()
                        Log.i(
                            "BluetoothGattCallback",
                            "Wrote to characteristic $uuidControl | value: ${this?.value}"
                        )
                    }
                    BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> {
                        Log.e("BluetoothGattCallback", "Write exceeded connection ATT MTU!")
                    }
                    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> {
                        Log.e("BluetoothGattCallback", "Write not permitted for $uuidControl!")
                    }
                    else -> {
                        Log.e(
                            "BluetoothGattCallback",
                            "Characteristic write failed for $uuidControl, error: $status"
                        )
                    }
                }
            }

        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            with(characteristic) {
                listener?.characteristicChanged(gatt, characteristic)
                Log.i(
                    "BluetoothGattCallback",
                    "Characteristic $uuidStateNotify changed | value: ${this?.value}"
                )
            }
        }
    }
    // Methods for writing, reading and sub/un-sub to notify character-changes
    fun connectToBleDevice(result: ScanResult): BluetoothGatt{
        return result.device.connectGatt(this, false, gattCallback)
    }

    private fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, payload: ByteArray) {
        val writeType = when { // check necessary capability's of the characteristic
            characteristic.isWritable() -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            characteristic.isWritableWithoutResponse() -> {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            }
            else -> error("Characteristic ${characteristic.uuid} cannot be written to")
        }

        bluetoothGatt?.let { gatt ->
            characteristic.writeType = writeType
            characteristic.value = payload
            gatt.writeCharacteristic(characteristic)
        } ?: error("Not connected to a BLE device!")
    }

    fun writeDescriptor(descriptor: BluetoothGattDescriptor, payload: ByteArray) {
        bluetoothGatt?.let { gatt ->
            descriptor.value = payload
            gatt.writeDescriptor(descriptor)
        } ?: error("Not connected to a BLE device!")
    }

    fun enableNotifications(characteristic: BluetoothGattCharacteristic) {
        val payload = when {
            characteristic.isIndicatable() -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            characteristic.isNotifiable() -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> {
                Log.e(
                    "ConnectionManager",
                    "${characteristic.uuid} doesn't support notifications/indications"
                )
                return
            }
        }

        characteristic.getDescriptor(cccDescriptorUuid)?.let { cccDescriptor ->
            if (bluetoothGatt?.setCharacteristicNotification(characteristic, true) == false) {
                Log.e(
                    "ConnectionManager",
                    "setCharacteristicNotification failed for ${characteristic.uuid}"
                )
                return
            }
            writeDescriptor(cccDescriptor, payload)
        } ?: Log.e(
            "ConnectionManager",
            "${characteristic.uuid} doesn't contain the CCC descriptor!"
        )
    }

    fun disableNotifications(characteristic: BluetoothGattCharacteristic) {
        if (!characteristic.isNotifiable() && !characteristic.isIndicatable()) {
            Log.e(
                "ConnectionManager",
                "${characteristic.uuid} doesn't support indications/notifications"
            )
            return
        }

        characteristic.getDescriptor(cccDescriptorUuid)?.let { cccDescriptor ->
            if (bluetoothGatt?.setCharacteristicNotification(characteristic, false) == false) {
                Log.e(
                    "ConnectionManager",
                    "setCharacteristicNotification failed for ${characteristic.uuid}"
                )
                return
            }
            writeDescriptor(cccDescriptor, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
        } ?: Log.e(
            "ConnectionManager",
            "${characteristic.uuid} doesn't contain the CCC descriptor!"
        )
    }
}