package xyz.emlyn.snapnote.ui.settings

import android.animation.ValueAnimator
import android.app.TaskStackBuilder
import android.content.*
import android.content.Context.MODE_PRIVATE
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import xyz.emlyn.snapnote.MainActivity
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

            activity!!.findViewById<LinearLayout>(R.id.themeLight).setOnClickListener(this::themeOnclick)
            activity!!.findViewById<LinearLayout>(R.id.themeDark).setOnClickListener(this::themeOnclick)
            activity!!.findViewById<LinearLayout>(R.id.themeSys).setOnClickListener(this::themeOnclick)

            activity!!.findViewById<LinearLayout>(R.id.themeLight).backgroundTintList = ColorStateList.valueOf(context!!.getColor(R.color.note_background_accent))
            activity!!.findViewById<LinearLayout>(R.id.themeDark).backgroundTintList = ColorStateList.valueOf(context!!.getColor(R.color.note_background_accent))
            activity!!.findViewById<LinearLayout>(R.id.themeSys).backgroundTintList = ColorStateList.valueOf(context!!.getColor(R.color.note_background_accent))


            val currentThemeMode = sp.getInt("theme", 2)
            if (currentThemeMode == 0) {
                // light
                activity!!.findViewById<LinearLayout>(R.id.themeLight).backgroundTintList =
                    ColorStateList.valueOf(context!!.getColor(R.color.note_background_accent_pale))
            } else if (currentThemeMode == 1) {
                // dark
                activity!!.findViewById<LinearLayout>(R.id.themeDark).backgroundTintList =
                    ColorStateList.valueOf(context!!.getColor(R.color.note_background_accent_pale))
            } else if (currentThemeMode == 2) {
                // follow system default
                activity!!.findViewById<LinearLayout>(R.id.themeSys).backgroundTintList =
                    ColorStateList.valueOf(context!!.getColor(R.color.note_background_accent_pale))
            }


        }, 10)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun themeOnclick(v : View) {

        var newThemeId = -1
        val vLL = (v as LinearLayout)

        activity!!.findViewById<LinearLayout>(R.id.themeLight).backgroundTintList = ColorStateList.valueOf(context!!.getColor(R.color.note_background_accent))
        activity!!.findViewById<LinearLayout>(R.id.themeDark).backgroundTintList = ColorStateList.valueOf(context!!.getColor(R.color.note_background_accent))
        activity!!.findViewById<LinearLayout>(R.id.themeSys).backgroundTintList = ColorStateList.valueOf(context!!.getColor(R.color.note_background_accent))

        v.backgroundTintList = ColorStateList.valueOf(context!!.getColor(R.color.note_background_accent_pale))
        if ((vLL.getChildAt(0) as TextView).text == getString(R.string.light)) {
            newThemeId = 0
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else if ((vLL.getChildAt(0) as TextView).text == getString(R.string.dark)) {
            newThemeId = 1
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else if ((vLL.getChildAt(0) as TextView).text == getString(R.string.system)) {
            newThemeId = 2
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        sp.edit().putInt("theme", newThemeId).apply()
        activity!!.findViewById<ConstraintLayout>(R.id.container).invalidate()
        //TaskStackBuilder.create(activity)
            //.addNextIntent(Intent(activity, MainActivity::class.java))
            //.addNextIntent(activity!!.intent)
            //.startActivities()

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
