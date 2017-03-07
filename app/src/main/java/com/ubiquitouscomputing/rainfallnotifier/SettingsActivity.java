package com.ubiquitouscomputing.rainfallnotifier;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.model.LatLng;
import com.ubiquitouscomputing.rainfallnotifier.service.CreateAlarmTask;

public class SettingsActivity extends PreferenceActivity {

    private static final String TAG = SettingsActivity.class.getSimpleName();
    static int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    public static class SettingsFragment extends PreferenceFragment {

        SharedPreferences prefs;
        SwitchPreference notificationToggle;
        CheckBoxPreference currentLocToggle;
        ListPreference notificationTimeSelect;
        Preference locationSelect;
        private static final int SETTINGS_ACTIVITY_LOCATION_REQUEST = 2;

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            //Retrieve saved preferences and preference objects for use later...
            prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            currentLocToggle = (CheckBoxPreference) findPreference(getString(R.string.current_location_toggle_key));
            notificationToggle = (SwitchPreference) findPreference(getString(R.string.notification_toggle_key));
            notificationTimeSelect = (ListPreference) findPreference(getString(R.string.notification_time_select_key));

            locationSelect = findPreference(getString(R.string.location_select_key));

            //Retrieve the saved location and set as Location Select preference summary
            String savedLocation =
                    prefs.getString(getString(R.string.saved_location_address),
                                         getString(R.string.default_location_select_summary));
            locationSelect.setSummary(savedLocation);


            //OnClick listener for Location Select to launch Google Places search
            locationSelect.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {

                    try {

                        //Filter to show only cities and localities in results
                        AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
                                .setTypeFilter(AutocompleteFilter.TYPE_FILTER_CITIES)
                                .build();

                        //Build the autocomplete intent and run it
                        Intent intent =
                                new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                                        .setFilter(typeFilter)
                                        .build(getActivity());
                        startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);

                    }
                    catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
                        Log.i(TAG, e.getMessage()); //Be grand
                    }
                    return true;
                }
            });

            //OnPreferenceChange listener
            Preference.OnPreferenceChangeListener prefChangeListener = new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {

                    //Retrieve notificationTime preference
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    String notificationTime = prefs.getString(getString(R.string.notification_time_select_key),
                            getString(R.string.default_notification_time));


                    //Create or cancel the alarm on Notification Toggle change
                    if(preference.equals(notificationToggle) && newValue.equals(true)) {

                        //Start a new alarm task
                        new CreateAlarmTask(getActivity()).startAlarm(notificationTime);
                    }
                    else if(preference.equals(notificationToggle) && newValue.equals(false)) {
                        new CreateAlarmTask(getActivity()).cancelAlarms();
                    }


                    //Restart the alarm if time preference is changed
                    if(preference.equals(notificationTimeSelect)) {
                        CreateAlarmTask alarm = new CreateAlarmTask(getActivity());

                        alarm.cancelAlarms();               //Cancel any existing alarms
                        alarm.startAlarm(notificationTime); //Restart the alarm with new time
                    }


                    //Check location permissions on enabling of current location checkbox
                    if(preference.equals(currentLocToggle) && newValue.equals(true)) {
                        notificationToggle.setEnabled(true);
                        checkLocationPermission();
                    }
                    //Disable & Turn off the notification toggle if current location checkbox
                    //is unchecked and no custom location has been selected
                    else if(preference.equals(currentLocToggle) &&
                            newValue.equals(false) &&
                            !prefs.contains(getString(R.string.saved_location_name))) {
                        notificationToggle.setChecked(false);
                        notificationToggle.setEnabled(false);
                    }

                    return true;
                }
            };

            //Set OnChangeListener for notification switch and current location checkbox
            notificationToggle.setOnPreferenceChangeListener(prefChangeListener);
            currentLocToggle.setOnPreferenceChangeListener(prefChangeListener);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {

            //Verify the autocomplete request
            if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {

                //If result is valid
                if (resultCode == RESULT_OK) {

                    //Get the address and LatLng of the selected location
                    Place place = PlaceAutocomplete.getPlace(getActivity(), data);
                    String name = place.getName().toString();
                    String address = place.getAddress().toString();
                    LatLng coordinates = place.getLatLng();

                    //Set the Location Select preference summary to the new place name
                    locationSelect.setSummary(address);

                    //Save the new place name, address and coordinates
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(getString(R.string.saved_location_name), name);
                    editor.putString(getString(R.string.saved_location_address), address);
                    editor.putFloat(getString(R.string.saved_location_latitude), (float)coordinates.latitude);
                    editor.putFloat(getString(R.string.saved_location_longitude), (float)coordinates.longitude);
                    editor.commit();

                    //Re-enable the notification toggle if current location mode is disabled
                    if(!currentLocToggle.isChecked()) {
                        notificationToggle.setEnabled(true);
                    }
                }
                //If result is an error
                else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                    Status status = PlaceAutocomplete.getStatus(getActivity(), data);
                    Log.i(TAG, status.getStatusMessage());
                }
                //If user cancels the search
                else if (resultCode == RESULT_CANCELED) {

                    //If no location has been saved previously, re-enable current location toggle
                    boolean savedLocation = prefs.contains(getString(R.string.saved_location_name));
                    if(!savedLocation) {
                        currentLocToggle.setChecked(true);
                        locationSelect.setSummary(getString(R.string.default_location_select_summary));
                    }
                }
            }
        }

        private void checkLocationPermission() {

            //Check if fine location permissions have been granted
            if (ContextCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                //If not granted, request permission
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        SETTINGS_ACTIVITY_LOCATION_REQUEST);
            }
        }

//        @Override
//        public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
//            switch (requestCode) {
//                case SETTINGS_ACTIVITY_LOCATION_REQUEST: {
//
//                    //If permission was denied, disable the current location feature
//                    if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
//                        Toast.makeText(getActivity(), "Location permissions are required for the current location feature to work", Toast.LENGTH_LONG).show();
//                        currentLocToggle.setChecked(false);
//                    }
//                }
//            }
//        }
    }
}