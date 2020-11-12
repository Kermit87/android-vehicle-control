package Data

import Enums.ConnectionState
import Enums.MoveMode
import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData

class ViewModel(application: Application): AndroidViewModel(application) {

    private val repository = Repository.getInstance(application.applicationContext)
    //private val blueToothDevices: LiveData<List<BluetoothDevice>>
    private val connectionState: LiveData<ConnectionState>
    private val receiveMessage: LiveData<String>
    private val moveMode: LiveData<MoveMode>

    init {

        //blueToothDevices = repository.getBlueToothDevices()
        connectionState = repository.connectionState()
        receiveMessage = repository.getReceiveMessages()
        moveMode = repository.currentMoveMode()
    }

    fun getConnectionState(): LiveData<ConnectionState>{
        return connectionState
    }

    fun getReceivedMessages(): LiveData<String>{
        return receiveMessage
    }

    fun currentMoveMode(): LiveData<MoveMode> {
        return moveMode
    }

    fun setMoveMode(mode: MoveMode){
        repository.setMoveMode(mode)
    }

    fun disconnectToGatt(){
        repository.disconnectToGatt()
    }

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