package com.example.robotikcontrol

import Data.VehicleMotion
import Data.ViewModel
import Enums.ConnectionState
import Enums.ContainerMotion
import Enums.Direction
import Enums.Orientation
import Screens.ControlFragment
import OldClasses.SettingsFragment
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer

class MainActivity : AppCompatActivity() ,
    View.OnClickListener, ControlFragment.TouchControlPanelCallback {

    private val viewModel: ViewModel by viewModels<ViewModel>()
    private lateinit var scanButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var statusIcon: ImageView
    private var currentVehicleMotion: VehicleMotion? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.hide()

        scanButton = findViewById(R.id.connectButton)
        disconnectButton = findViewById(R.id.disconnectButton)
        statusIcon = findViewById(R.id.statusIcon)
        scanButton.setOnClickListener(this)
        disconnectButton.setOnClickListener(this)

        viewModel.getConnectionState().observe(this,connectionObserver)
        currentVehicleMotion = VehicleMotion(Orientation.RELEASE,Direction.UNKNOWN,0)
        showControl()
        //setDeviceBluetoothDiscoverable()
        allowLocationDetectionPermissions()
        viewModel.scanService()
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            FINE_LOCATION_PERMISSION_REQUEST -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    //scanLeDevice(true)
                    viewModel?.scanService()
                } else {
                    //tvTestNote.text= getString(R.string.allow_location_detection)
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

    private fun setDeviceBluetoothDiscoverable() {
        //no need to request bluetooth permission if  discoverability is requested
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        discoverableIntent.putExtra(
            BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
            0
        )// 0 to keep it always discoverable
        startActivity(discoverableIntent)
    }

    /*fun showSettings(){
        supportFragmentManager.beginTransaction().addToBackStack(null).
        replace(R.id.main_frame, SettingsFragment()).commit()
    }*/

    fun showControl(){
        supportFragmentManager.beginTransaction().
        replace(R.id.main_frame,ControlFragment()).commit()
    }

    private fun sendVehicleMotionOrderToBTD(vehicleMotion: VehicleMotion){
        //val json = createVehicleMotionJson(vehicleMotion.orientation,
          //  vehicleMotion.direction,vehicleMotion.speed)
        val commandString = createVehicleMotionString(vehicleMotion.orientation,
            vehicleMotion.direction,vehicleMotion.speed)
        // TODO nur zu testzwecken
        //val str = json.toString().length.toString()
        //viewModel.writeStringCharacter(str)

        viewModel.writeStringCharacter(commandString)
    }

    private fun sendContainerMotionToBTD(motion: ContainerMotion){
        val commandString = createContainerMotionString(motion)
        viewModel.writeStringCharacter(commandString)
    }

    override fun onClick(v: View?) {
        //showSettings()
        when(v?.id){
            R.id.disconnectButton -> viewModel.disconnectToGatt()
            R.id.connectButton -> viewModel.scanService()
        }
    }

    // Callbacks from control panel fragment
    // Callbacks for the speed panel
    override fun speedPanelTouched(event: MotionEvent?) {
        val definedSpeed = getPredefinedSpeed(event)
        val definedOrientation = getPredefinedOrientation(event)
        if (definedSpeed != currentVehicleMotion?.speed ||
            definedOrientation != currentVehicleMotion?.orientation){
            currentVehicleMotion?.speed = definedSpeed
            currentVehicleMotion?.orientation = definedOrientation
            currentVehicleMotion?.let { vehicleMotion ->
                sendVehicleMotionOrderToBTD(vehicleMotion)
            }
        }
    }

    override fun speedPanelMoved(event: MotionEvent?) {
        val definedSpeed = getPredefinedSpeed(event)
        val definedOrientation = getPredefinedOrientation(event)
        if (definedSpeed != currentVehicleMotion?.speed ||
            definedOrientation != currentVehicleMotion?.orientation){
            currentVehicleMotion?.speed = definedSpeed
            currentVehicleMotion?.orientation = definedOrientation
            currentVehicleMotion?.let { vehicleMotion ->
                sendVehicleMotionOrderToBTD(vehicleMotion)
            }
        }
    }

    override fun speedPanelRelease(event: MotionEvent?) {
        currentVehicleMotion?.speed = 0
        currentVehicleMotion?.orientation = Orientation.RELEASE
        currentVehicleMotion?.let { vehicleMotion ->
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

    override fun containerControlButtonPressed(id: Int) {
        val containerMotion = when(id){
            R.id.topButton -> ContainerMotion.UP
            R.id.downButton -> ContainerMotion.DOWN
            else -> ContainerMotion.STOP
        }
        sendContainerMotionToBTD(containerMotion)
    }

    override fun containerControlButtonRelease(id: Int) {
        sendContainerMotionToBTD(ContainerMotion.STOP)
    }

    // Observer to update the UI
    private val connectionObserver = Observer<ConnectionState> { state ->
        if (state == ConnectionState.Connected){
            statusIcon.setImageResource(R.drawable.green_round_bg)
        }
        if (state == ConnectionState.Disconnected) {
            statusIcon.setImageResource(R.drawable.red_round_bg)
        }
    }

    companion object{
        private const val FINE_LOCATION_PERMISSION_REQUEST= 1001
    }
}
