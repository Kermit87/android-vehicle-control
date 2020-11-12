package Data

import Enums.ConnectionState
import Enums.MoveMode
import Network.*
import OldClasses.SerialService
import android.bluetooth.*
import android.bluetooth.le.ScanResult
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import kotlin.collections.ArrayList

class Repository private constructor(private var context: Context): BLEListener, ServiceConnection {

    //private val blueToothDevices: MediatorLiveData<List<BluetoothDevice>> = MediatorLiveData()
    private val receiveMessage: MediatorLiveData<String> = MediatorLiveData()
    private val connectionState: MediatorLiveData<ConnectionState> = MediatorLiveData() // to observe the connection state
    private val moveMode: MediatorLiveData<MoveMode> = MediatorLiveData()
    //private val scanResults: MediatorLiveData<ScanResult> = MediatorLiveData()
    private var btService: BLEService? = null
    private var tryToConnect = false // we don't want start a new scanning if the last scanning is still running


    init {
        //blueToothDevices.postValue(pairedDevices())
        connectionState.postValue(ConnectionState.Disconnected)
        moveMode.postValue(MoveMode.Stop)

        Intent(context, SerialService::class.java).also { intent ->
            if (btService == null) {
                context.startService(intent)
                btService = BLEService(context).apply { attach(this@Repository) }
            }
            context.bindService(intent, this, Context.BIND_AUTO_CREATE)
        }

    }
    //######### Live Data to observe #########
    /*fun getBlueToothDevices(): LiveData<List<BluetoothDevice>>{
        return blueToothDevices
    }*/

    fun getReceiveMessages(): LiveData<String>{
        return receiveMessage
    }

    fun connectionState(): LiveData<ConnectionState>{
        return connectionState
    }

    fun currentMoveMode(): LiveData<MoveMode>{
        return moveMode
    }

    fun setMoveMode(mode: MoveMode){
        moveMode.postValue(mode)
    }

    /*fun pairedDevices(): ArrayList<BluetoothDevice>?{
        val btAdapter = BluetoothAdapter.getDefaultAdapter() ?: return null
        if(!btAdapter.isEnabled){return null}

        val list = ArrayList<BluetoothDevice>()
        btAdapter.bondedDevices.forEach{ device ->
            list.add(device)
        }
        return list
    }*/

    /*fun connectToBTDevice(address: String){
        try {
            val device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address)
            connectionState.postValue(ConnectionState.Pending)
            btService?.connect(SerialSocket(context.applicationContext, device))
        }catch (e: Exception){
            onSerialConnectError(e)
        }
    }

    fun writeToBTDevice(msg: String){
        if (connectionState.value != ConnectionState.Connected){
            Toast.makeText(context, "not connected", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val bytes = msg.toByteArray()
            btService?.write(bytes)
        } catch (e: java.lang.Exception){
            onSerialIoError(e)
        }
    }*/

    fun scanService(){
        btService?.scanLeDevice(true)
    }

    fun disconnectToGatt(){
        btService?.disconnectToGatt()
    }

    fun writeStringCharacter(msg: String){
        btService?.writeStringCharacterToBLEService(msg)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        //btService = (service as SerialService.SerialBinder).service
        btService = (service as BLEService.BLEBinder).getService()
        btService?.attach(this)
    }

    override fun onServiceDisconnected(name: ComponentName?) {

    }

    /*
    override fun onSerialConnect() {
        connectionState.postValue(ConnectionState.Connected)
    }

    override fun onSerialConnectError(e: Exception?) {
        connectionState.postValue(ConnectionState.Disconnected)
    }

    override fun onSerialRead(data: ByteArray?) {
        val msg = if (data != null){
            String(data)
        }else{ "error" }
        receiveMessage.postValue(msg)
    }

    override fun onSerialIoError(e: Exception?) {

    }
     */
    override fun connected(gatt: BluetoothGatt?) {
        connectionState.postValue(ConnectionState.Connected)
        tryToConnect = false
        btService?.discoverServices(gatt)
    }

    override fun disconnected() {
        connectionState.postValue(ConnectionState.Disconnected)
        tryToConnect = false
    }

    override fun writeCharacteristicSuccessfully() {
        Log.e("CharacteristicWritten", "characteristic successfully written with")
    }

    override fun scanResult(callbackType: Int, result: ScanResult?) {
        //val uuids = result?.device?.uuids
        //uuids?.first { it.uuid == UUID.fromString("19B10000-E8F2-537E-4F6C-D104768A1214") } ?: return
        val name = result?.device?.name
        val uuids = result?.device?.uuids

        if (result == null){
            return
        }
        if (name == "LED"){
            Log.e("RepositoryScanResult", "Device with service found")
            Log.e("RepositoryScanResult", "Name: $name")
        }
        if (connectionState?.value == ConnectionState.Disconnected && !tryToConnect) {
            btService?.connectToGatt(result)
            tryToConnect = true
        }
    }

    override fun serviceDiscoveredResult(gatt: BluetoothGatt?, status: Int) {

    }

    override fun characteristicChanged(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?
    ) {
        val str = characteristic?.value ?: return
        receiveMessage.postValue(str.toString())
    }

    companion object {
        // Singleton Support
        private var instance: Repository? = null

        fun getInstance(context: Context) =
            instance ?: synchronized(this){
                instance ?: Repository(context).also { instance = it }
            }
    }
}