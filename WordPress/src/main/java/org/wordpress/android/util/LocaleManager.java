package org.wordpress.android.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Pair;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Helper class for working with localized strings. Ensures updates to the users
 * selected language is properly saved and resources appropriately updated for the
 * android version.
 */
public class LocaleManager {
    /**
     * Length of a {@link String} (representing a language code) when there is no region included.
     * For example: "en" contains no region, "en_US" contains a region (US)
     * <p>
     * Used to parse a language code {@link String} when creating a {@link Locale}.
     */
    private static final int NO_REGION_LANG_CODE_LEN = 2;

    /**
     * Index of a language code {@link String} where the region code begins. The language code
     * format is cc_rr, where cc is the country code (e.g. en, es, az) and rr is the region code
     * (e.g. us, au, gb).
     */
    private static final int REGION_SUBSTRING_INDEX = 3;

    /**
     * Key used for saving the language selection to shared preferences.
     */
    private static final String LANGUAGE_KEY = "language-pref";

    /**
     * Activate the locale associated with the provided context.
     * @param context The current context.
     */
    public static Context setLocale(Context context) {
        return updateResources(context, getLanguage(context));
    }

    /**
     * Change the active locale to the language provided. Save the updated language
     * settings to sharedPreferences.
     * @param context The current context
     * @param language The 2-letter language code (example "en") to switch to
     */
    public static void setNewLocale(Context context, String language) {
        if (isSameLanguage(language)) {
            return;
        }
        saveLanguageToPref(context, language);
        updateResources(context, language);
    }

    /**
     * Compare the language for the current context with another language.
     * @param language The language to compare
     * @return True if the languages are the same, else false
     */
    public static boolean isSameLanguage(@NonNull String language) {
        Locale newLocale = languageLocale(language);
        return Locale.getDefault().getLanguage().equals(newLocale.getLanguage());
    }

    /**
     * If the user has selected a language other than the device default, return that
     * language code, else just return the device default language code.
     * @return The 2-letter language code (example "en")
     */
    private static String getLanguage(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.contains(LANGUAGE_KEY)) {
            return prefs.getString(LANGUAGE_KEY, "");
        }
        return LanguageUtils.getCurrentDeviceLanguageCode(context);
    }

    /**
     * Save the updated language to SharedPreferences.
     * Use commit() instead of apply() to ensure the language preference is saved instantly
     * as the app may be restarted immediately.
     * @param context The current context
     * @param language The 2-letter language code (example "en")
     */
    @SuppressLint("ApplySharedPref")
    private static void saveLanguageToPref(Context context, String language) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(LANGUAGE_KEY, language).commit();
    }

    /**
     * Remove any saved custom language selection from SharedPreferences.
     * Use commit() instead of apply() to ensure the language preference is saved instantly
     * as the app may be restarted immediately.
     * @param context The current context
     */
    @SuppressLint("ApplySharedPref")
    private static void removePersistedLanguage(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().remove(LANGUAGE_KEY).commit();
    }

    /**
     * Update resources for the current session.
     *
     * @param context The current active context
     * @param language The 2-letter language code (example "en")
     * @return The modified context containing the updated localized resources
     */
    private static Context updateResources(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());

        // NOTE: Earlier versions of Android require both of these to be set, otherwise
        // RTL may not be implemented properly.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale);
            context = context.createConfigurationContext(config);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            config.locale = locale;
            res.updateConfiguration(config, res.getDisplayMetrics());
        }
        return context;
    }

    /**
     * Method gets around a bug in the java.util.Formatter for API 7.x as detailed here
     * [https://bugs.openjdk.java.net/browse/JDK-8167567]. Any strings that contain
     * locale-specific grouping separators should use:
     * <code>
     *     String.format(LocaleManager.getSafeLocale(context), baseString, val)
     * </code>
     *
     * An example of a string that contains locale-specific grouping separators:
     * <code>
     *     <string name="test">%,d likes</string>
     * </code>
     */
    public static Locale getSafeLocale(@Nullable Context context) {
        Locale baseLocale;
        if (context == null) {
            baseLocale = Locale.getDefault();
        } else {
            Configuration config = context.getResources().getConfiguration();
            baseLocale = Build.VERSION.SDK_INT >= 24 ? config.getLocales().get(0) : config.locale;
        }

        if (Build.VERSION.SDK_INT >= 24) {
            return languageLocale(baseLocale.getLanguage());
        } else {
            return baseLocale;
        }
    }

    /**
     * Gets a locale for the given language code.
     * @param languageCode The 2-letter language code (example "en"). If null or empty will return
     *                     the current default locale.
     */
    public static Locale languageLocale(@Nullable String languageCode) {
        if (TextUtils.isEmpty(languageCode)) {
            return Locale.getDefault();
        }

        if (languageCode.length() > NO_REGION_LANG_CODE_LEN) {
            return new Locale(languageCode.substring(0, NO_REGION_LANG_CODE_LEN),
                    languageCode.substring(REGION_SUBSTRING_INDEX));
        }

        return new Locale(languageCode);
    }

    /**
     * Creates a map from language codes to WordPress language IDs.
     */
    public static Map<String, String> generateLanguageMap(Context context) {
        String[] languageIds = context.getResources().getStringArray(org.wordpress.android.R.array.lang_ids);
        String[] languageCodes = context.getResources().getStringArray(org.wordpress.android.R.array.language_codes);

        Map<String, String> languageMap = new HashMap<>();
        for (int i = 0; i < languageIds.length && i < languageCodes.length; ++i) {
            languageMap.put(languageCodes[i], languageIds[i]);
        }

        return languageMap;
    }

    /**
     * Generates display strings for given language codes. Used as entries in language preference.
     */
    @android.support.annotation.Nullable
    public static Pair<String[], String[]> createSortedLanguageDisplayStrings(CharSequence[] languageCodes,
                                                                              Locale locale) {
        if (languageCodes == null || languageCodes.length < 1) {
            return null;
        }

        ArrayList<String> entryStrings = new ArrayList<>(languageCodes.length);
        for (int i = 0; i < languageCodes.length; ++i) {
            // "__" is used to sort the language code with the display string so both arrays are sorted at the same time
            entryStrings.add(i, StringUtils.capitalize(
                    getLanguageString(languageCodes[i].toString(), locale)) + "__" + languageCodes[i]);
        }

        Collections.sort(entryStrings, Collator.getInstance(locale));

        String[] sortedEntries = new String[languageCodes.length];
        String[] sortedValues = new String[languageCodes.length];

        for (int i = 0; i < entryStrings.size(); ++i) {
            // now, we can split the sorted array to extract the display string and the language code
            String[] split = entryStrings.get(i).split("__");
            sortedEntries[i] = split[0];
            sortedValues[i] = split[1];
        }

        return new Pair<>(sortedEntries, sortedValues);
    }

    /**
     * Generates detail display strings in the currently selected locale. Used as detail text
     * in language preference dialog.
     */
    @android.support.annotation.Nullable
    public static String[] createLanguageDetailDisplayStrings(CharSequence[] languageCodes) {
        if (languageCodes == null || languageCodes.length < 1) {
            return null;
        }

        String[] detailStrings = new String[languageCodes.length];
        for (int i = 0; i < languageCodes.length; ++i) {
            detailStrings[i] = StringUtils.capitalize(getLanguageString(
                    languageCodes[i].toString(), languageLocale(languageCodes[i].toString())));
        }

        return detailStrings;
    }

    /**
     * Return a non-null display string for a given language code.
     */
    public static String getLanguageString(String languageCode, Locale displayLocale) {
        if (languageCode == null || languageCode.length() < 2 || languageCode.length() > 6) {
            return "";
        }

        Locale languageLocale = languageLocale(languageCode);
        String displayLanguage = StringUtils.capitalize(languageLocale.getDisplayLanguage(displayLocale));
        String displayCountry = languageLocale.getDisplayCountry(displayLocale);

        if (!TextUtils.isEmpty(displayCountry)) {
            return displayLanguage + " (" + displayCountry + ")";
        }
        return displayLanguage;
    }
}
