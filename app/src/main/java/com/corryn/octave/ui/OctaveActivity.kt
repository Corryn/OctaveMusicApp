package com.corryn.octave.ui

import android.os.Bundle
import android.os.PersistableBundle
import android.view.LayoutInflater
import com.corryn.octave.databinding.ActivityOctaveBinding
import com.corryn.octave.ui.base.BaseActivity

class OctaveActivity: BaseActivity<ActivityOctaveBinding>() {

    override val viewBindingInflater: (LayoutInflater) -> ActivityOctaveBinding
        get() = ActivityOctaveBinding::inflate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupNavController(binding.navHostFragment)
    }

}