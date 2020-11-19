package Screens

import Data.ViewModel
import Enums.ConnectionState
import Enums.MoveMode
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.robotikcontrol.R

class ControlFragment: Fragment(), View.OnClickListener, View.OnTouchListener {

    private val viewModel: ViewModel by viewModels<ViewModel>()
    private lateinit var rightButton: Button
    private lateinit var leftButton: Button
    private lateinit var speedView: ImageView
    private lateinit var cordView: TextView
    private lateinit var connectButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var listener: ControlFragmentCallback

    private var speedPanelInUse: Boolean = false
    private var leftRightButtonInUse: Boolean = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        val v = inflater.inflate(R.layout.control_fragment, container, false)
        rightButton = v.findViewById(R.id.rightButton)
        leftButton = v.findViewById(R.id.leftButton)
        connectButton = v.findViewById(R.id.connectButton)
        disconnectButton = v.findViewById(R.id.disconnectButton)
        speedView = v.findViewById(R.id.speedView)
        cordView = v.findViewById(R.id.cordView)

        connectButton.setOnClickListener(this)
        disconnectButton.setOnClickListener(this)

        //rightButton.setOnClickListener(this)
        rightButton.setOnTouchListener(this)
        //leftButton.setOnClickListener(this)
        leftButton.setOnTouchListener(this)
        speedView.setOnTouchListener(this)

        //viewModel.getReceivedMessages().observe(viewLifecycleOwner, btMessageObserver)
        viewModel.getConnectionState().observe(viewLifecycleOwner, connectionObserver)
        viewModel.currentMoveMode().observe(viewLifecycleOwner,moveModeObserver)
        return v
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {

        if (v?.id == R.id.leftButton || v?.id == R.id.rightButton){
            when(event?.actionMasked){

                MotionEvent.ACTION_DOWN -> {
                    listener.directionButtonPressed(v.id)
                    leftRightButtonInUse = true
                    val resID = when(v?.id){
                        R.id.leftButton -> R.drawable.control_button_left_pressed
                        R.id.rightButton -> R.drawable.control_button_right_pressed
                        else -> 0
                    }
                    v.setBackgroundResource(resID)
                }
                MotionEvent.ACTION_UP -> {
                    listener.directionButtonRelease(v.id)
                    leftRightButtonInUse = false
                    val resID = when(v?.id){
                        R.id.leftButton -> R.drawable.control_button_left
                        R.id.rightButton -> R.drawable.control_button_right
                        else -> 0
                    }
                    v.setBackgroundResource(resID)
                }
            }
        }

        if (v?.id == R.id.speedView){
            val height = v.height
            val width = v.width
            val y = event?.y
            val x = event?.x
            val str = "x: $x  -  y: $y -  height: $height"
            cordView.text = str
            when(event?.actionMasked){
                MotionEvent.ACTION_DOWN -> {
                    listener.speedPanelTouched(height,event)
                    speedPanelInUse = true
                }
                MotionEvent.ACTION_UP -> {
                    listener.speedPanelRelease(event)
                    speedPanelInUse = false
                }
                MotionEvent.ACTION_MOVE -> {
                    listener.speedPanelMoved(height,event)
                speedPanelInUse = true
                }
            }
        }
        return true
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.connectButton -> listener.connectButtonClicked()
            R.id.disconnectButton -> listener.disconnectButtonClicked()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as ControlFragmentCallback
        } catch (e: ClassCastException){
            throw ClassCastException(
                context.toString()
                        + " must implement OnContentAButtonClickedListener"
            )
        }
    }

    private val connectionObserver = Observer<ConnectionState> { state ->
        when (state) {
            ConnectionState.Connected -> {
                connectButton.isEnabled = false
                disconnectButton.isEnabled = true
                connectButton.setTextColor(resources.getColor(R.color.connectButtonDisable, context?.theme))
                disconnectButton.setTextColor(resources.getColor(R.color.disconnectButtonEnable, context?.theme))
            }
            ConnectionState.Disconnected -> {
                connectButton.isEnabled = true
                disconnectButton.isEnabled = false
                connectButton.setTextColor(resources.getColor(R.color.connectButtonEnable, context?.theme))
                disconnectButton.setTextColor(resources.getColor(R.color.disconnectButtonDisable, context?.theme))
            }
            else -> {}
        }
    }

    private val moveModeObserver = Observer<MoveMode> { mode ->
        if (mode != null){
            val resID = when(mode){
                MoveMode.Stop -> R.drawable.speed_control
                MoveMode.ForwardSpeed1 -> R.drawable.speed_control_forward1
                MoveMode.ForwardSpeed2 -> R.drawable.speed_control_forward2
                MoveMode.BackwardSpeed1 -> R.drawable.speed_control_backward1
                MoveMode.BackwardSpeed2 -> R.drawable.speed_control_backward2
            }
            speedView.setImageResource(resID)
        }
    }

    interface ControlFragmentCallback {
        fun speedPanelMoved(viewHeight:Int, event: MotionEvent?)
        fun speedPanelTouched(viewHeight:Int, event: MotionEvent?)
        fun speedPanelRelease(event: MotionEvent?)
        fun directionButtonPressed(id: Int)
        fun directionButtonRelease(id: Int)
        fun connectButtonClicked()
        fun disconnectButtonClicked()
    }
}