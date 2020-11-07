package Data

import Enums.ConnectionState
import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData

class ViewModel(application: Application): AndroidViewModel(application) {

    private val repository = Repository.getInstance(application.applicationContext)
    //private val blueToothDevices: LiveData<List<BluetoothDevice>>
    private val connectionState: LiveData<ConnectionState>
    private val receiveMessage: LiveData<String>

    init {

        //blueToothDevices = repository.getBlueToothDevices()
        connectionState = repository.connectionState()
        receiveMessage = repository.getReceiveMessages()
    }

    /*fun getBlueToothDevices(): LiveData<List<BluetoothDevice>>{
        return blueToothDevices
    }*/

    fun getConnectionState(): LiveData<ConnectionState>{
        return connectionState
    }

    fun getReceivedMessages(): LiveData<String>{
        return receiveMessage
    }

    fun disconnectToGatt(){
        repository.disconnectToGatt()
    }

    /*fun connectToBTDevice(address: String){
        repository.connectToBTDevice(address)
    }

    fun writeToBTDevice(msg: String){
        repository.writeToBTDevice(msg)
    }*/

    fun scanService(){
        repository.scanService()
    }

    fun writeStringCharacter(msg: String){
        repository.writeStringCharacter(msg)
    }


    companion object {
        // Singleton Support
        private var instance: ViewModel? = null

        fun getInstance(application: Application) =
            instance ?: synchronized(this){
                instance ?: ViewModel(application).also { instance = it }
            }
    }
}