package com.example.robotikcontrol

import Data.VehicleMotion
import Data.ViewModel
import Enums.ConnectionState
import Enums.Direction
import Enums.MoveMode
import Enums.Orientation
import Screens.ControlFragment
import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import kotlinx.coroutines.*


class MainActivity : AppCompatActivity() ,
    View.OnClickListener, ControlFragment.ControlFragmentCallback {

    private val viewModel: ViewModel by viewModels<ViewModel>()
    private lateinit var statusIcon: ImageView
    private var currentVehicleMotion: VehicleMotion? = null // save current movement
    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.hide()
        // init ui-elements
        statusIcon = findViewById(R.id.statusIcon)
        // for observing an connection state change
        viewModel.getConnectionState().observe(this,connectionObserver)
        currentVehicleMotion = VehicleMotion(Orientation.RELEASE,Direction.UNKNOWN,0)
        showControl()
        //setDeviceBluetoothDiscoverable()
        allowLocationDetectionPermissions() // is needed to be sure ble is active
    }
    // is called if a result is returned from another activity
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            FINE_LOCATION_PERMISSION_REQUEST -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    //scanLeDevice(true)
                    viewModel?.scanService()
                } else {
                }
                return
            }
        }
    }

    private fun allowLocationDetectionPermissions() {
        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this@MainActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), FINE_LOCATION_PERMISSION_REQUEST)
        }
    }

    fun showControl(){
        supportFragmentManager.beginTransaction().
        replace(R.id.main_frame,ControlFragment()).commit()
    }

    private fun sendVehicleMotionOrderToBTD(vehicleMotion: VehicleMotion){
        val commandString = createVehicleMotionString(vehicleMotion.orientation,
            vehicleMotion.direction,vehicleMotion.speed)
        viewModel.writeStringCharacter(commandString)
    }

    private fun showConnectingPending(){

        job = CoroutineScope(Dispatchers.Main).launch {
            var count = 0
            while (true){
                when(count){
                    0 -> statusIcon.setImageResource(R.drawable.ic_connect_icon1)
                    1 -> statusIcon.setImageResource(R.drawable.ic_connect_icon2)
                    2 -> statusIcon.setImageResource(R.drawable.ic_connect_icon3)
                    3 -> statusIcon.setImageResource(R.drawable.ic_connect_icon4)
                }
                count++
                if (count == 4){count = 0}
                delay(700)
            }
        }
        job?.start()
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.disconnectButton -> viewModel.disconnectToGatt()
            R.id.connectButton -> viewModel.scanService()
        }
    }

    override fun connectButtonClicked() {
        viewModel.scanService()
    }

    override fun disconnectButtonClicked() {
        viewModel.disconnectToGatt()
    }

    // Callbacks from control panel fragment
    // Callbacks for the speed panel
    override fun speedPanelTouched(viewHeight:Int,event: MotionEvent?) {
        val definedSpeed = getPredefinedSpeed(viewHeight,event)
        val definedOrientation = getPredefinedOrientation(event)
        // only send move order if there are some changes
        if (definedSpeed != currentVehicleMotion?.speed ||
            definedOrientation != currentVehicleMotion?.orientation){
            currentVehicleMotion?.speed = definedSpeed
            currentVehicleMotion?.orientation = definedOrientation
            currentVehicleMotion?.let { vehicleMotion ->
                viewModel.setMoveMode(calculateMoveMode(definedOrientation, definedSpeed))
                sendVehicleMotionOrderToBTD(vehicleMotion)
            }
        }
    }

    override fun speedPanelMoved(viewHeight:Int,event: MotionEvent?) {
        val definedSpeed = getPredefinedSpeed(viewHeight,event)
        val definedOrientation = getPredefinedOrientation(event)
        // only send move order if there are some changes
        if (definedSpeed != currentVehicleMotion?.speed ||
            definedOrientation != currentVehicleMotion?.orientation){
            currentVehicleMotion?.speed = definedSpeed
            currentVehicleMotion?.orientation = definedOrientation
            currentVehicleMotion?.let { vehicleMotion ->
                viewModel.setMoveMode(calculateMoveMode(definedOrientation, definedSpeed))
                sendVehicleMotionOrderToBTD(vehicleMotion)
            }
        }
    }

    override fun speedPanelRelease(event: MotionEvent?) {
        currentVehicleMotion?.speed = 0
        currentVehicleMotion?.orientation = Orientation.RELEASE
        currentVehicleMotion?.let { vehicleMotion ->
            viewModel.setMoveMode(MoveMode.Stop)
            sendVehicleMotionOrderToBTD(vehicleMotion)
        }
    }
    // Callbacks for the buttons
    override fun directionButtonRelease(id: Int) {
        currentVehicleMotion?.direction = Direction.STRAIGHT
        currentVehicleMotion?.let { vehicleMotion ->
            sendVehicleMotionOrderToBTD(vehicleMotion)
        }
    }

    override fun directionButtonPressed(id: Int) {
        val direction = when(id){
            R.id.rightButton ->  Direction.RIGHT
            R.id.leftButton -> Direction.LEFT
            else -> Direction.STRAIGHT
        }
        if (direction != currentVehicleMotion?.direction){
            currentVehicleMotion?.direction = direction
            currentVehicleMotion?.let { vehicleMotion ->
                sendVehicleMotionOrderToBTD(vehicleMotion)
            }
        }
    }

    // Observer for updating the status icon
    private val connectionObserver = Observer<ConnectionState> { state ->
        if (state == null){return@Observer}

        when(state){
            ConnectionState.Connected -> {
                job?.cancel()
                statusIcon.setImageResource(R.drawable.ic_connected_icon)
            }
            ConnectionState.Disconnected -> {
                job?.cancel()
                statusIcon.setImageResource(R.drawable.ic_disconnect_icon3)
            }
            ConnectionState.Pending -> {
                showConnectingPending()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.scanService()
    }

    companion object{
        private const val FINE_LOCATION_PERMISSION_REQUEST= 1001
    }
}




/*private fun setDeviceBluetoothDiscoverable() {
    //no need to request bluetooth permission if  discoverability is requested
    val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
    discoverableIntent.putExtra(
        BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
        0
    )// 0 to keep it always discoverable
    startActivity(discoverableIntent)
}*/

/*fun showSettings(){
    supportFragmentManager.beginTransaction().addToBackStack(null).
    replace(R.id.main_frame, SettingsFragment()).commit()
}*/


/*override fun containerControlButtonPressed(id: Int) {
    val containerMotion = when(id){
        R.id.topButton -> ContainerMotion.UP
        R.id.downButton -> ContainerMotion.DOWN
        else -> ContainerMotion.STOP
    }
    sendContainerMotionToBTD(containerMotion)
}*/