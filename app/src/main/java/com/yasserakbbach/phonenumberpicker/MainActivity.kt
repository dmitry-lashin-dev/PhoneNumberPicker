package com.yasserakbbach.phonenumberpicker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.yasserakbbach.phonenumberpicker.adapters.OnCountrySelected
import com.yasserakbbach.phonenumberpicker.databinding.ActivityMainBinding
import com.yasserakbbach.phonenumberpicker.models.Country

class MainActivity : AppCompatActivity(), OnCountrySelected {

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.phoneNumber.apply {
            //onlyContinents(Continent.AFRICA)
            //exceptCountries("eh")
            setOnCountrySelected(this@MainActivity)
            //setTextColor("#00FF00")
            setDefaultCountry("CA")
            //Log.d(TAG, "selected: ${getSelectedCountry()}")
            //setMaxLength(5)
            setOnPhoneFieldFocusListener { hasFocus ->
                if (!hasFocus) {
                    val code = getSelectedCountry().countryCodeFormatted
                    val phoneData = getPhoneData()
                    Log.d(
                        "PHONE_DATA",
                        "code = $code, phone = ${phoneData.first}, phoneCode = ${phoneData.second}"
                    )
                }
            }
        }
    }

    override fun setOnCountrySelected(country: Country) {

        Toast.makeText(applicationContext, country.toString(), Toast.LENGTH_SHORT).show()
    }
}