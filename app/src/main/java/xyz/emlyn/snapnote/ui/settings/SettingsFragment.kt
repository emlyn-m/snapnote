package xyz.emlyn.snapnote.ui.settings

import android.animation.ValueAnimator
import android.content.*
import android.content.Context.MODE_PRIVATE
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.switchmaterial.SwitchMaterial
import xyz.emlyn.snapnote.R
import xyz.emlyn.snapnote.databinding.FragmentSettingsBinding


class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var root : View

    private lateinit var sp : SharedPreferences
    private lateinit var importAction : ActivityResultLauncher<String>
    private lateinit var exportAction : ActivityResultLauncher<String>

    private var deleteIcoAnim : ValueAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ViewModelProvider(this)[SettingsViewModel::class.java]

        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        root = binding.root

        activity!!.findViewById<Toolbar>(R.id.toolbar).getChildAt(1).visibility = View.GONE

        sp = activity!!.getSharedPreferences("xyz.emlyn.snapnote", MODE_PRIVATE)
        Handler(Looper.getMainLooper()).postDelayed({
            activity!!.findViewById<SwitchCompat>(R.id.registerAsCamera).setOnCheckedChangeListener(this::cameraShortcut)
            activity!!.findViewById<SwitchCompat>(R.id.registerAsCamera).isChecked = sp.getBoolean("cameraShortcut", false)

        }, 10)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun cameraShortcut(switch : CompoundButton, newState : Boolean) {
        sp.edit().putBoolean("cameraShortcut", newState).apply()

        val pm: PackageManager = context!!.packageManager
        val compName =
            ComponentName(getString(R.string.package_name), getString(R.string.package_name) + ".CameraShortcut")
        if (newState) {
            pm.setComponentEnabledSetting(
                compName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        } else {
            pm.setComponentEnabledSetting(
                compName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }


}
