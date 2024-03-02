package com.corryn.octave.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

// TODO See if there's any way to improve on this base class
abstract class BaseFragment<V : ViewBinding> : Fragment() {

    abstract val viewBindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> V

    private var _binding: V? = null
    val binding: V
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = viewBindingInflater(inflater, container, false)

        return _binding?.root
    }

    fun initializeBackPressListener(listener: () -> Unit) {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    listener.invoke()
                }
            }
        )
    }

}