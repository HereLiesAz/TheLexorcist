package com.hereliesaz.lexorcist.fragments.main

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.hereliesaz.lexorcist.databinding.FragmentMainBinding
import com.hereliesaz.lexorcist.model.StringConstants

class MainFragment : Fragment() {
    private lateinit var binding: FragmentMainBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    private fun setup() {
        setOnClickListeners()
    }

    private fun generateDescription(script: String): String {
        Log.d("MainFragment", "Generating description for script: $script")
        val includesRegex = """evidence\.(?:text|content)\.includes\(\"([^\"]+)\"\)""".toRegex()
        // Corrected regex definition using a standard string with escaped characters
        val tagsRegex = "parser\\.tags\\.push\\(\\\"([^\\\"]+)\\\"\\)".toRegex()

        val includesMatches = includesRegex.findAll(script).mapNotNull { it.groupValues.getOrNull(1) }.toList()
        val tagsMatches = tagsRegex.findAll(script).mapNotNull { it.groupValues.getOrNull(1) }.toList()

        if (includesMatches.isNotEmpty() && tagsMatches.isNotEmpty()) {
            val includesStr = includesMatches.joinToString(", ")
            val tagsStr = tagsMatches.joinToString(", ")
            return "This script tags evidence that includes \'${includesStr}\' with the tag \'${tagsStr}\'."
        }

        return "This script does not have a generated description."
    }

    private fun setOnClickListeners() {
        val buttons = mapOf(
            binding.fragmentMainAction1Button to StringConstants.String.Action1,
            binding.fragmentMainAction2Button to StringConstants.String.Action2,
            binding.fragmentMainAction3Button to StringConstants.String.Action3,
            binding.fragmentMainAction4Button to StringConstants.String.Action4,
            binding.fragmentMainAction5Button to StringConstants.String.Action5,
            binding.fragmentMainAction6Button to StringConstants.String.Action6
        )

        for ((button, text) in buttons) {
            button.setOnClickListener {
                binding.fragmentMainScriptTextInputEditText.append(text)
            }
        }

        binding.fragmentMainGenerateDescriptionButton.setOnClickListener {
            val script = binding.fragmentMainScriptTextInputEditText.text.toString()
            val description = generateDescription(script)
            binding.fragmentMainScriptDescriptionTextInputEditText.setText(description)
        }
    }

    companion object {
        fun newInstance() = MainFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        setup()
        return binding.root
    }
}
