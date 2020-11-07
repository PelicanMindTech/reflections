package com.paulaslab.reflections

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.FragmentTransaction

/**
 * A simple [Fragment] subclass.
 * Use the [InitialFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class InitialFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val fragmentManager = this.parentFragmentManager
        val app = activity?.application as ReflectionsApp
        val id = app.journalStore!!.newEntryId()
        val file = app.journalStore!!.getEntryFile(id)

        val journallingFragment = JournallingFragment.newInstance(this, file, id)

        val view = inflater.inflate(R.layout.fragment_initial, container, false)

        view?.findViewById<Button>(R.id.start_journalling_button)?.setOnTouchListener(
            View.OnTouchListener { view, motionEvent ->
                when (motionEvent.action) {
                    MotionEvent.ACTION_UP -> {
                        val ft: FragmentTransaction = fragmentManager.beginTransaction()
                        ft.replace(R.id.flContainer, journallingFragment)
                        ft.commit()
                    }
                }
                false
        })

        return view
    }

    companion object {
        @JvmStatic
        fun newInstance() = InitialFragment()
    }
}