package com.corryn.octave.ui.base

import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding

// TODO See if there's any way to improve on this base class; is it necessary?
abstract class BaseActivity<V: ViewBinding>: AppCompatActivity() {

    abstract val viewBindingInflater: (LayoutInflater) -> V
    val binding by lazy { viewBindingInflater(layoutInflater) }

    lateinit var navController: NavController
        private set

    fun setupNavController(fragmentContainerView: FragmentContainerView) {
        supportFragmentManager.findFragmentById(fragmentContainerView.id)
            ?.also { fragment ->
                navController = fragment.findNavController()
            }
    }

}