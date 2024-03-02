package com.corryn.octave.ui.base

import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding

abstract class BaseActivity<V: ViewBinding>: AppCompatActivity() {

    abstract val viewBindingInflater: (LayoutInflater) -> V
    val binding by lazy { viewBindingInflater(layoutInflater) }

}