package com.thewalkingschoolbus.thewalkingschoolbus;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.Nullable;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.thewalkingschoolbus.thewalkingschoolbus.Interface.OnTaskComplete;
import com.thewalkingschoolbus.thewalkingschoolbus.Models.GpsLocation;
import com.thewalkingschoolbus.thewalkingschoolbus.Models.User;
import com.thewalkingschoolbus.thewalkingschoolbus.api_binding.GetUserAsyncTask;
import com.thewalkingschoolbus.thewalkingschoolbus.service.UploadLocationStopService;
import com.thewalkingschoolbus.thewalkingschoolbus.map_modules.MapUtil;
import com.thewalkingschoolbus.thewalkingschoolbus.service.UploadLocationService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.thewalkingschoolbus.thewalkingschoolbus.api_binding.GetUserAsyncTask.functionType.POST_GPS_LOCATION;

public class WalkingFragment extends android.app.Fragment {

    private static final String TAG = "WalkingFragment";
    private View view;
    private boolean isWalking = false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (container != null) {
            container.removeAllViews();
        }
        view = inflater.inflate(R.layout.fragment_walking, container, false);
        setupBtn();
        MapUtil.getLocationPermission();

        return view;
    }

    private void setupBtn() {
        Button startBtn = view.findViewById(R.id.startWalkingBtn);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startWalk();
            }
        });
        Button arrivedBtn = view.findViewById(R.id.stopWalkingBtn);
        arrivedBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopWalk();
            }
        });
        Button panicBtn = view.findViewById(R.id.panicBtn);
        panicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                panicWalk();
            }
        });
    }

    private void startWalk() {
        if (isWalking) {
            Toast.makeText(getActivity(), "Already walking", Toast.LENGTH_SHORT).show();
        } else {
            isWalking = true;
            Toast.makeText(getActivity(), "Start", Toast.LENGTH_SHORT).show();
            updateStatusText("Now walking...");
            updateLocationFirstTime();
            startUploadLocationService();
        }
    }

    private void stopWalk() {
        if (isWalking) {
            isWalking = false;
            Toast.makeText(getActivity(), "Stop", Toast.LENGTH_SHORT).show();
            updateStatusText("Walk stopped.");
            cancelUploadLocationService();
        } else {
            Toast.makeText(getActivity(), "Not walking", Toast.LENGTH_SHORT).show();
        }
    }

    private void panicWalk() {

    }

    private void startUploadLocationService() {
        Intent intent = new Intent(getActivity(), UploadLocationService.class);
        getActivity().stopService(intent);
        getActivity().startService(intent);
    }

    private void cancelUploadLocationService() {
        Intent intent = new Intent(getActivity(), UploadLocationService.class);
        getActivity().stopService(intent);

        intent = new Intent(getActivity(), UploadLocationStopService.class);
        getActivity().stopService(intent);
        getActivity().startService(intent);
    }

    private void updateStatusText(String statusText) {
        TextView text = view.findViewById(R.id.walkStatusTxt);
        text.setText(statusText);
    }

    // DATE

    public static Date stringToDate(String dateInString) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.000Z'");
        try {
            return format.parse(dateInString);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String dateToStringSimple(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("'DATE: 'yyyy-MM-dd', TIME:'HH:mm:ss");
        return dateFormat.format(date);
    }

    public static String dateToString(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.000Z'");
        return dateFormat.format(date);
    }

    // STATIC

    public static void uploadCurrentCoordinate(Location currentLocation) {
        User user = User.getLoginUser();

        if (currentLocation == null) {
            Log.d(TAG, "#### current location == null! (note: first upload may be null)");
            user.setLastGpsLocation(new GpsLocation(null, null, null));
        } else {
            Double lat = currentLocation.getLatitude();
            Double lng = currentLocation.getLongitude();
            String timestamp = dateToString(new Date());

            Log.d(TAG, "#### LAT / LNG: " + lat + " / " + lng);
            Log.d(TAG, "#### TIMESTAMP: " + timestamp);

            user.setLastGpsLocation(new GpsLocation(lat.toString(), lng.toString(), timestamp));
        }

        new GetUserAsyncTask(POST_GPS_LOCATION, user, null, null,null, new OnTaskComplete() {
            @Override
            public void onSuccess(Object result) {
                //User user = (User) result;
                Log.d(TAG, "#### Successfully updated current location. " + result);
            }
            @Override
            public void onFailure(Exception e) {
                Log.d(TAG, "#### Error: "+e.getMessage());
            }
        }).execute();
    }

    private void updateLocationFirstTime() {
        final LocationManager locationManager = (LocationManager) getActivity().getSystemService(getActivity().LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(getActivity(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,
                new android.location.LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        uploadCurrentCoordinate(location);
                        Log.d(TAG, "####: First Update");
                        locationManager.removeUpdates(this);
                    }

                    @Override
                    public void onStatusChanged(String s, int i, Bundle bundle) {

                    }

                    @Override
                    public void onProviderEnabled(String s) {

                    }

                    @Override
                    public void onProviderDisabled(String s) {

                    }
                });
    }

    public static Intent makeIntent(Context context) {
        return new Intent(context, WalkingFragment.class);
    }
}
