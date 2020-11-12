package Screens

import Data.ViewModel
import Enums.MoveMode
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
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
    private lateinit var listener: TouchControlPanelCallback

    private var speedPanelInUse: Boolean = false
    private var leftRightButtonInUse: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        val v = inflater.inflate(R.layout.control_fragment, container, false)
        rightButton = v.findViewById(R.id.rightButton)
        leftButton = v.findViewById(R.id.leftButton)
        speedView = v.findViewById(R.id.speedView)
        cordView = v.findViewById(R.id.cordView)

        rightButton.setOnClickListener(this)
        rightButton.setOnTouchListener(this)
        leftButton.setOnClickListener(this)
        leftButton.setOnTouchListener(this)
        speedView.setOnTouchListener(this)

        viewModel.getReceivedMessages().observe(viewLifecycleOwner, btMessageObserver)
        viewModel.currentMoveMode().observe(viewLifecycleOwner,moveModeObserver)
        return v
    }


    override fun onTouch(v: View?, event: MotionEvent?): Boolean {

        if (v?.id == R.id.leftButton || v?.id == R.id.rightButton){
            when(event?.actionMasked){
                MotionEvent.ACTION_DOWN -> {
                    listener.directionButtonPressed(v.id)
                    leftRightButtonInUse = true
                }
                MotionEvent.ACTION_UP -> {
                    listener.directionButtonRelease(v.id)
                    leftRightButtonInUse = false
                }
            }
        }

        if (v?.id == R.id.speedView){
            val y = event?.y
            val x = event?.x
            val str = "x: $x  -  y: $y"
            cordView.text = str
            when(event?.actionMasked){
                MotionEvent.ACTION_DOWN -> {
                    listener.speedPanelTouched(event)
                    speedPanelInUse = true
                }
                MotionEvent.ACTION_UP -> {
                    listener.speedPanelRelease(event)
                    speedPanelInUse = false
                }
                MotionEvent.ACTION_MOVE -> {
                    listener.speedPanelMoved(event)
                speedPanelInUse = true
                }
            }
        }
        //checkUIState()
        return true
    }

    override fun onClick(v: View?) {
        //listener.controlButtonPressed(v?.id ?: 0)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as TouchControlPanelCallback
        } catch (e: ClassCastException){
            throw ClassCastException(
                context.toString()
                        + " must implement OnContentAButtonClickedListener"
            )
        }
    }

    private val btMessageObserver = Observer<String> { msg ->
        if (msg != null){
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
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

    interface TouchControlPanelCallback {
        fun speedPanelMoved(event: MotionEvent?)
        fun speedPanelTouched(event: MotionEvent?)
        fun speedPanelRelease(event: MotionEvent?)
        fun directionButtonPressed(id: Int)
        fun directionButtonRelease(id: Int)
    }
}