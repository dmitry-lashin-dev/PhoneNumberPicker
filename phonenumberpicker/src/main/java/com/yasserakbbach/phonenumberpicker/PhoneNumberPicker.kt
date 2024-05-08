package com.yasserakbbach.phonenumberpicker

import android.content.Context
import android.graphics.Color
import android.text.InputFilter
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.redmadrobot.inputmask.MaskedTextChangedListener
import com.redmadrobot.inputmask.helper.AffinityCalculationStrategy
import com.yasserakbbach.phonenumberpicker.databinding.CountryListBinding
import com.yasserakbbach.phonenumberpicker.databinding.PhoneNumberPickerBinding
import com.yasserakbbach.phonenumberpicker.models.Country
import com.yasserakbbach.phonenumberpicker.adapters.CountryAdapter
import com.yasserakbbach.phonenumberpicker.adapters.OnCountrySelected
import com.yasserakbbach.phonenumberpicker.models.Continent
import com.yasserakbbach.phonenumberpicker.utils.CountryFactory
import com.yasserakbbach.phonenumberpicker.utils.CountryPattern
import com.yasserakbbach.phonenumberpicker.utils.FormatNumberUtils
import com.yasserakbbach.phonenumberpicker.utils.disableCopyPaste
import java.util.*

/**
 * Basic widget built on Material Text Input to wrap picking phone number.
 *
 * @author: Yasser AKBBACH
 */
open class PhoneNumberPicker(context: Context, private val attrs: AttributeSet?) :
    LinearLayout(context, attrs),
    CountryAdapter.Presenter {

    private var textChangedListener: MaskedTextChangedListener? = null
    private val numberUtils by lazy { FormatNumberUtils(context) }
    private var phoneChangCallback: ((phoneAndIso: Pair<String, String>) -> Unit)? = null
    private var shouldBlockCountrySelectionEvent: Boolean = false

    /**
     * To keep track of the selected country
     */
    private lateinit var mSelectedCountry: Country

    /**
     * Reference of the layout to handle the UI changes
     */
    protected val binding: PhoneNumberPickerBinding by lazy {
        PhoneNumberPickerBinding.inflate(
            LayoutInflater.from(context), this, false
        )
    }

    /**
     * List of the current countries
     */
    private lateinit var mCountries: List<Country>

    /**
     * Bottom sheet dialog the wraps the countries + search of country by name
     */
    private val countryBS: BottomSheetDialog by lazy {
        BottomSheetDialog(context).apply {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.isDraggable = false
            behavior.isFitToContents = false
        }
    }

    /**
     * Bottom sheet layout of country list
     */
    private val countryBinding: CountryListBinding by lazy {
        CountryListBinding.inflate(LayoutInflater.from(context), this, false)
    }

    /**
     * Listener of country selection
     */
    private var mOnCountrySelected: OnCountrySelected? = null

    /**
     * To initialize everything
     */
    init {

        addView(binding.root)
        binding.apply {
            ivCountryFlag.setOnClickListener { showCountrySelectionDialog() }
            ivSelectArrow.setOnClickListener { showCountrySelectionDialog() }
        }
        initAttributes()
        preventDeletion(mSelectedCountry.countryCodeFormatted)
        focusSelectionToEnd()
    }

    private fun showCountrySelectionDialog() {
        if (!shouldBlockCountrySelectionEvent) {
            initCountryList()
        }
    }

    /**
     * Init attributes passed from XML
     */
    private fun initAttributes() {

        context?.withStyledAttributes(attrs, R.styleable.PhoneNumberPicker) {
            val textColor = getColor(R.styleable.PhoneNumberPicker_textColor, DEFAULT_TEXT_COLOR)
            val textSize =
                getDimensionPixelSize(R.styleable.PhoneNumberPicker_textSize, DEFAULT_TEXT_SIZE)
            val defaultCountry =
                getString(R.styleable.PhoneNumberPicker_defaultCountry) ?: DEFAULT_COUNTRY_KEY
            val continents =
                getInteger(R.styleable.PhoneNumberPicker_continents, DEFAULT_CONTINENT_KEY)

            mCountries = CountryFactory.onlyContinents(continents)

            binding.apply {
                etPhoneNumber.disableCopyPaste()
                etPhoneNumber.setTextColor(textColor)
                etPhoneNumber.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())
                mSelectedCountry = Country.byIso2(defaultCountry, mCountries) ?: mCountries[0]
                loadSelectedCountry()
            }
        }
    }

    /**
     * Get the current country list
     */
    val currentCountries: List<Country>
        get() = mCountries

    /**
     * Init country list
     */
    private fun initCountryList() {

        val countriesAdapter = CountryAdapter(this)
        countriesAdapter.submitList(mCountries)
        countryBinding.apply {
            countryList.adapter = countriesAdapter
            searchView.setQuery("", false)
        }

        countryBS.apply {
            setContentView(countryBinding.root)
            show()
        }

        // Search query
        countryBinding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {

                if (query != null) {

                    countriesAdapter.submitList(mCountries.filter {
                        it.name.lowercase(Locale.ENGLISH)
                            .contains(
                                query.lowercase(
                                    Locale.ENGLISH
                                )
                            )
                    })
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {

                if (newText != null) {

                    countriesAdapter.submitList(mCountries.filter {
                        it.name.lowercase(Locale.ENGLISH)
                            .contains(
                                newText.lowercase(
                                    Locale.ENGLISH
                                )
                            )
                    })
                }
                return true
            }
        })
    }

    /**
     * Handle on country click
     */
    override fun onCountryClick(country: Country) {

        mSelectedCountry = country
        loadSelectedCountry()
        //val etPhoneNumber = binding.etPhoneNumber
        val countryCode = country.countryCodeFormatted

        focusSelectionToEnd()

        // Prevent deleting country code
        preventDeletion(countryCode)

        countryBS.hide()
        mOnCountrySelected?.setOnCountrySelected(country)
    }

    /**
     * Prevent deletion on certain point to avoid country code getting wiped
     */
    private fun preventDeletion(countryCode: String) {

        binding.etPhoneNumber.apply {
            setOnKeyListener { _, keyCode, _ ->

                KeyEvent.KEYCODE_DEL == keyCode && countryCode.length == this.text.toString().length
            }
        }
    }

    /**
     * Focus input to the last input character
     */
    private fun focusSelectionToEnd() {

        binding.apply {
            etPhoneNumber.requestFocus()
            etPhoneNumber.setOnClickListener {
                etPhoneNumber.setSelection(etPhoneNumber.text.toString().length)
            }
        }
    }

    /**
     * Render the selected country's flag + country code
     */
    private fun loadSelectedCountry() {
        binding.apply {
            ivCountryFlag.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    loadCountryFlag(mSelectedCountry.resourceNameDrawable)
                )
            )
            etPhoneNumber.removeTextChangedListener(textChangedListener)
            val formattedNumber = numberUtils.formatPhoneNumber(
                numberUtils.getExampleNumber(mSelectedCountry.iso2)
            )
            val pattern = CountryPattern.create(
                formattedNumber.orEmpty()
            )
            textChangedListener = MaskedTextChangedListener.installOn(
                etPhoneNumber,
                pattern,
                emptyList(),
                AffinityCalculationStrategy.WHOLE_STRING,
                object : MaskedTextChangedListener.ValueListener {
                    override fun onTextChanged(
                        maskFilled: Boolean,
                        extractedValue: String,
                        formattedValue: String
                    ) {
                        Log.i(
                            "PHONE",
                            "extractedValue = $extractedValue formattedValue = $formattedValue "
                        )
                        phoneChangCallback?.invoke(extractedValue to mSelectedCountry.iso2)
                    }
                }
            )
            etPhoneNumber.setText(mSelectedCountry.countryCodeFormatted)
        }
    }

    /**
     * Get the drawable id of a given drawable name
     */
    private fun loadCountryFlag(drawableName: String) =
        context.resources.getIdentifier(drawableName, "drawable", context.packageName)

    /**
     * Get full phone number (with +)
     */
    fun getFullPhoneNumber() =
        binding.etPhoneNumber.text.toString()

    /**
     * Get the selected country
     */
    fun getSelectedCountry() = mSelectedCountry

    /**
     * Filter the countries according to passed continents,
     * For more details check [Continent]
     * XML attribute:
     * app:continents="all|africa|asia|europe|north_america|south_america|oceania"
     *
     */
    fun onlyContinents(vararg continents: Continent) {

        mCountries = CountryFactory.onlyContinents(*continents)
    }

    /**
     * Exclude certain countries by iso2 criteria,
     * Check [CountryFactory] to get ISO2s
     */
    fun exceptCountries(vararg iso2s: String) {

        mCountries = mCountries.filter {
            it.iso2 !in iso2s
        }
    }

    /**
     * Add listener on country selection
     */
    fun setOnCountrySelected(onCountrySelected: OnCountrySelected) {
        mOnCountrySelected = onCountrySelected
    }

    /**
     * Change text color by a resource color,
     * XML attribute: app:textColor="@color/black"
     */
    fun setTextColor(@ColorRes color: Int) {

        binding.etPhoneNumber.setTextColor(
            ContextCompat.getColor(context, color)
        )
    }

    /**
     * Change text color by hexadecimal color
     * e.g: #FF0000 (Red color)
     * XML attribute: app:textColor="#FF0000"
     */
    fun setTextColor(color: String) {

        binding.etPhoneNumber.setTextColor(Color.parseColor(color))
    }

    /**
     * Change text size, the used unit is PIXEL
     * XML attribute: app:textSize="18sp"
     */
    fun setTextSize(size: Float) {

        binding.etPhoneNumber.setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
    }

    /**
     * Set default country flag by given iso2,
     * NB: In case the countries were filtered according to specific continents and the given flag wasn't there
     * the default flag would be the first country of the current country list
     */
    fun setDefaultCountry(iso2: String) {

        mSelectedCountry = Country.byIso2(iso2, mCountries) ?: mCountries[0]
        loadSelectedCountry()
    }

    /**
     * To change max length of digits including the [+] digit!
     * XML attribute: app:maxLength="14"
     */
    fun setMaxLength(maxLength: Int) {

        binding.etPhoneNumber.filters = arrayOf(InputFilter.LengthFilter(maxLength))
    }

    fun setCustomBackground(@DrawableRes backId: Int) {
        binding.root.setBackgroundResource(backId)
    }

    fun setPhoneChangeCallback(callback: (Pair<String, String>) -> Unit) {
        phoneChangCallback = callback
    }

    fun removePhoneChangeCallback() {
        phoneChangCallback = null
    }

    fun clearPhone() {
        with(binding) {
            etPhoneNumber.text?.clear()
            etPhoneNumber.setText(mSelectedCountry.countryCodeFormatted)
        }
    }

    fun setPhoneNumber(phone: String) {
        numberUtils.getCountryIsoCode(phone)?.let { isoCode ->
            numberUtils.parsePhoneNumber(phone, isoCode)?.let { number ->
                numberUtils.formatPhoneNumber(number)?.let {
                    binding.etPhoneNumber.setText(phone)
                }
            }
        } ?: binding.etPhoneNumber.setText(phone)
    }

    fun setAbilityToBlockCountryFlag(shouldBlock: Boolean) {
        shouldBlockCountrySelectionEvent = shouldBlock
        binding.ivSelectArrow.isVisible = !shouldBlock
    }

    fun setDividerColor(@ColorRes colorId: Int) {
        binding.phoneDivider.setBackgroundColor(ContextCompat.getColor(context, colorId))
    }

    fun setPhoneDividerVisibility(isVisible: Boolean) {
        binding.phoneDivider.isVisible = isVisible
    }

    fun setPickerDividerVisibility(isVisible: Boolean) {
        binding.pickerDivider.isVisible = isVisible
    }

    fun setPickerDividerColor(@ColorRes colorId: Int){
        binding.pickerDivider.setBackgroundColor(ContextCompat.getColor(context, colorId))
    }

    companion object {

        var DEFAULT_TEXT_COLOR = R.color.black
        var DEFAULT_OUTLINE_BORDER_COLOR = R.color.outlineBorderColor
        const val DEFAULT_TEXT_SIZE = 18
        const val DEFAULT_COUNTRY_KEY = "ma"
        const val DEFAULT_CONTINENT_KEY = 63
        const val DEFAULT_MAX_LENGTH = 14
    }
}