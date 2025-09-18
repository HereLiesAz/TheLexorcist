package com.hereliesaz.lexorcist.fragments.main

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.hereliesaz.lexorcist.model.StringConstants

class MainFragment : Fragment() {
    private lateinit var binding: FragmentMainBinding
    private var caller: MainFragmentListener? = null

    private fun setup() {
        setOnClickListeners()
    }

    private fun generateDescription(script: String): String {
        val includesRegex = """evidence\.(?:text|content)\.includes\("([^"]+)"\)""".toRegex()
        val tagsRegex = """parser\.tags\.push\("([^"]+)"\)""".toRegex()

        val includesMatches = includesRegex.findAll(script).map { it.groupValues[1] }.toList()
        val tagsMatches = tagsRegex.findAll(script).map { it.groupValues[1] }.toList()

        if (includesMatches.isNotEmpty() && tagsMatches.isNotEmpty()) {
            val includesStr = includesMatches.joinToString(", ")
            val tagsStr = tagsMatches.joinToString(", ")
            return "This script tags evidence that includes '${includesStr}' with the tag '${tagsStr}'."
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainFragmentListener) {
            caller = context
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)

        setup()

        return binding.root
    }
}