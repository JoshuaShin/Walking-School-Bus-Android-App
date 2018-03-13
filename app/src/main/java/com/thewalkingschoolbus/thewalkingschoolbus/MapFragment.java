package com.thewalkingschoolbus.thewalkingschoolbus;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import com.thewalkingschoolbus.thewalkingschoolbus.Interface.OnTaskComplete;
import com.thewalkingschoolbus.thewalkingschoolbus.Models.EnterGroupNameDialogFragment;
import com.thewalkingschoolbus.thewalkingschoolbus.Models.Group;
import com.thewalkingschoolbus.thewalkingschoolbus.Models.User;
import com.thewalkingschoolbus.thewalkingschoolbus.api_binding.GetUserAsyncTask;
import com.thewalkingschoolbus.thewalkingschoolbus.map_modules.DirectionFinder;
import com.thewalkingschoolbus.thewalkingschoolbus.map_modules.DirectionFinderListener;
import com.thewalkingschoolbus.thewalkingschoolbus.map_modules.Route;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import static com.thewalkingschoolbus.thewalkingschoolbus.api_binding.GetUserAsyncTask.functionType.ADD_MEMBER_TO_GROUP;
import static com.thewalkingschoolbus.thewalkingschoolbus.api_binding.GetUserAsyncTask.functionType.CREATE_GROUP;
import static com.thewalkingschoolbus.thewalkingschoolbus.api_binding.GetUserAsyncTask.functionType.LIST_GROUPS;

/*
* SOURCES - Based on following tutorials:
* Google Maps Android API tutorial
*   https://developers.google.com/maps/documentation/android-api/current-place-tutorial
* Youtube tutorial "Get Device Location" by "CodingWithMitch"
*   https://www.youtube.com/watch?v=fPFr0So1LmI
* Youtube tutorial "Basic Google Maps API Android Tutorial + Google Maps Directions API" by "Hiep Mai Thanh"
*   https://www.youtube.com/watch?v=fPFr0So1LmI
* map_modules from source code by "Hiep Mai Thanh"
*   https://github.com/hiepxuan2008/GoogleMapDirectionSimple
*/
public class MapFragment extends android.support.v4.app.Fragment {

    private static final String TAG = "MapFragment";
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1234;
    private static final int ERROR_DIALOG_REQUEST = 9001;
    private static final float DEFAULT_ZOOM = 18;

    private GoogleMap mMap;
    private Boolean mLocationPermissionGranted = false;
    private Boolean mValidRouteEstablished = false;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private View view;

    private EditText etOrigin;
    private EditText etDestination;
    private List<Marker> originMarkers = new ArrayList<>();
    private List<Marker> destinationMarkers = new ArrayList<>();
    private List<Polyline> polylinePaths = new ArrayList<>();
    private ProgressDialog progressDialog;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (container != null) {
            container.removeAllViews();
        }
        view = inflater.inflate(R.layout.fragment_map, container, false);

        if (isServicesOK()) {
            getLocationPermission();
        }
        etOrigin = (EditText) view.findViewById(R.id.etOrigin);
        etDestination = (EditText) view.findViewById(R.id.etDestination);
        Button btnFindPath = (Button) view.findViewById(R.id.btnFindPath);
        btnFindPath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendRequest();
            }
        });
        Button btnCreateGroup = (Button) view.findViewById(R.id.btnCreateGroup);
        btnCreateGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayCreateGroupDialog();
            }
        });
        Button btnViewGroup = (Button) view.findViewById(R.id.btnViewGroup);
        btnViewGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayNearbyGroups();
            }
        });

        return view;
    }

    public boolean isServicesOK(){
        Log.d(TAG, "isServicesOK: checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getActivity());

        if(available == ConnectionResult.SUCCESS){
            //everything is fine and the user can make map requests
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;
        }
        else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            //an error occurred but we can resolve it
            Log.d(TAG, "isServicesOK: an error occurred but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(getActivity(), available, ERROR_DIALOG_REQUEST);
            dialog.show();
        }else{
            Toast.makeText(getActivity(), "You can't make map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private void getLocationPermission() {
    /*
     * Request location permission, so that we can get the location of the device.
     * The result of the permission request is handled by a callback, onRequestPermissionsResult.
     */
        Log.d(TAG, "getLocationPermission: getting location permissions");

        if (ContextCompat.checkSelfPermission(this.getActivity().getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
            initMap();
        } else {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: called.");

        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
                initMap();
            }
        }
    }

    private void initMap() {
        Log.d(TAG, "initMap: initializing map");

        SupportMapFragment mapFragment = (SupportMapFragment)getChildFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                Log.d(TAG, "onMapReady: map is ready");

                mMap = googleMap;
                mMap.setPadding(0, 600, 0, 0); // Boundaries for google map buttons
                if (mLocationPermissionGranted) {
                    getDeviceLocation();
                    if (ActivityCompat.checkSelfPermission(getActivity(),
                            android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(getActivity(),
                            android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            }
        });
    }

    private void getDeviceLocation() {
    /*
     * Get the best and most recent location of the device, which may be null in rare
     * cases when a location is not available.
     */
        Log.d(TAG, "getDeviceLocation: getting the devices current location");

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());
        try {
            if (mLocationPermissionGranted) {
                Task locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            Location currentLocation = (Location) task.getResult(); // TODO: BUG: Occasionally, even though task.isSuccessful, current location will return null and crash app.
                            if (currentLocation == null) {
                                Toast.makeText(getActivity(), "Cannot find current location. (error code 1)", Toast.LENGTH_SHORT).show();
                            } else {
                                moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM);
                            }
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            Toast.makeText(getActivity(), "Cannot find current location. (error code 2)", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        } catch(SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void moveCamera(LatLng latLng, float zoom) {
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude );
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    private void sendRequest() {
        String origin = etOrigin.getText().toString();
        String destination = etDestination.getText().toString();
        if (origin.isEmpty()) {
            Toast.makeText(getActivity(), "Please enter origin address!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (destination.isEmpty()) {
            Toast.makeText(getActivity(), "Please enter destination address!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Draw route
            new DirectionFinder(new DirectionFinderListener() {
                @Override
                public void onDirectionFinderStart() {
                    displayProgressDialog();

                    if (originMarkers != null) {
                        for (Marker marker : originMarkers) {
                            marker.remove();
                        }
                    }

                    if (destinationMarkers != null) {
                        for (Marker marker : destinationMarkers) {
                            marker.remove();
                        }
                    }

                    if (polylinePaths != null) {
                        for (Polyline polyline:polylinePaths ) {
                            polyline.remove();
                        }
                    }
                }

                @Override
                public void onDirectionFinderSuccess(List<Route> routes) {
                    mValidRouteEstablished = true;
                    progressDialog.dismiss();
                    polylinePaths = new ArrayList<>();
                    originMarkers = new ArrayList<>();
                    destinationMarkers = new ArrayList<>();

                    for (Route route : routes) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(route.startLocation, 16));
                        ((TextView) view.findViewById(R.id.tvDuration)).setText(route.duration.text);
                        ((TextView) view.findViewById(R.id.tvDistance)).setText(route.distance.text);

                        originMarkers.add(mMap.addMarker(new MarkerOptions()
                                //.icon(BitmapDescriptorFactory.fromResource(R.drawable.start_blue)) // TODO: SET CUSTOM MARKER
                                .title(route.startAddress)
                                .position(route.startLocation)));
                        destinationMarkers.add(mMap.addMarker(new MarkerOptions()
                                //.icon(BitmapDescriptorFactory.fromResource(R.drawable.end_green)) // TODO: SET CUSTOM MARKER
                                .title(route.endAddress)
                                .position(route.endLocation)));

                        PolylineOptions polylineOptions = new PolylineOptions().
                                geodesic(true).
                                color(getResources().getColor(R.color.logoBlue)).
                                width(10);

                        for (int i = 0; i < route.points.size(); i++)
                            polylineOptions.add(route.points.get(i));

                        polylinePaths.add(mMap.addPolyline(polylineOptions));
                    }
                }
            }, origin, destination).execute();

            // Save origin and destination coordinates
            geoLocate();

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void displayProgressDialog() {
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        progressDialog.setMessage("Finding direction...");
//        progressDialog.setCancelable(false);
//        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialogInterface, int i) {
//                progressDialog.dismiss();
//            }
//        });
        progressDialog.show();

//        progressDialog = ProgressDialog.show(getActivity(), "Please wait.",
//                "Finding direction..!", true);
    }

    private void displayCreateGroupDialog() {
        if (mValidRouteEstablished) {
            // Create dialog which calls createGroup when user enters valid group name.
            android.support.v4.app.FragmentManager manager = getFragmentManager();
            EnterGroupNameDialogFragment dialog = new EnterGroupNameDialogFragment();
            dialog.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
            dialog.show(manager, "MessageDialog");
        } else {
            Toast.makeText(getActivity(), "Please enter valid path.", Toast.LENGTH_SHORT).show();
        }
    }

    public static void createGroup(Context context) {


        // Add to group list using NAME, ORIGIN, DESTINATION
        Group group = new Group();
        group.setGroupDescription(null);
//        double[] lat;
//        double[] lng;
//               group.setRouteLatArray(lat);
//               group.setRouteLngArray(lng);

        new GetUserAsyncTask(CREATE_GROUP, null, null, group, null, new OnTaskComplete() {
            @Override
            public void onSuccess(Object result) {
                if(result == null){

                }else {
                    Group[] group = (Group[]) result;
                }
            }

            @Override
            public void onFailure(Exception e) {
                // leave empty
            }
        }).execute();

        // Transition to group window
    }


    private double[] currentRouteLatArray;
    private double[] currentRouteLngArray;

    private void geoLocate() {
        Log.d(TAG, "geoLocate: geolocating current route's origin and destination coordinates.");

        currentRouteLatArray = new double[2];
        currentRouteLngArray = new double[2];

        String origin = etOrigin.getText().toString();
        String destination = etDestination.getText().toString();

        Geocoder geocoder = new Geocoder(getActivity());
        List<Address> originList = new ArrayList<>();
        List<Address> destinationList = new ArrayList<>();
        try {
            originList = geocoder.getFromLocationName(origin, 1);
        } catch (IOException e) {
            Log.e(TAG, "geoLocate: IOException: " + e.getMessage());
        }
        try {
            destinationList = geocoder.getFromLocationName(destination, 1);
        } catch (IOException e) {
            Log.e(TAG, "geoLocate: IOException: " + e.getMessage());
        }
        // Save origin coordinates
        if (originList.size() > 0) {
            Log.d(TAG, "geoLocate: if 1");
            Address originAddress = originList.get(0);
            currentRouteLatArray[0] = originAddress.getLatitude();
            currentRouteLngArray[0] = originAddress.getLongitude();
        }
        // Save destination coordinates
        if (destinationList.size() > 0) {
            Log.d(TAG, "geoLocate: if 2");
            Address destinationAddress = destinationList.get(0);
            currentRouteLatArray[1] = destinationAddress.getLatitude();
            currentRouteLngArray[1] = destinationAddress.getLongitude();
        }

        Log.d(TAG, "geoLocate: " + currentRouteLatArray[0] + ", " + currentRouteLngArray[0] + ", " + currentRouteLatArray[1] + ", " + currentRouteLngArray[1]);
    }
    
    private static final int DEFAULT_SEARCH_RADIUS_METERS = 1000;

    private void displayNearbyGroups() {
        if (mValidRouteEstablished) {
            // Get list of groups from database // TODO: implement real group list from server
            new GetUserAsyncTask(LIST_GROUPS, null, null, null, null, new OnTaskComplete() {
                @Override
                public void onSuccess(Object result) {
                    if (result == null) {
                        // failed
                    }else{
                        Group[] groupList = (Group[]) result;
                    }
                }

                @Override
                public void onFailure(Exception e) {

                }
            }).execute();

            List<Group> groupList = MainMenuActivity.existingGroups;

            Log.d(TAG, "displayNearbyGroups: current destination Lat, Lng: " + currentRouteLatArray[1] + ", " + currentRouteLngArray[1]);
            for (Group group : groupList) {
                // Compare destination difference of current route input and current group being examined. Returns distance in meters in results.
                float[] results = new float[3];
                Log.d(TAG, "displayNearbyGroups: distance between destinations in meters(?): " + results[0] + ", " + results[1] + ", " + results[2]);
                Location.distanceBetween(currentRouteLatArray[1], currentRouteLngArray[1],
                        group.getRouteLatArray()[1], group.getRouteLngArray()[1],
                        results);
                Log.d(TAG, "displayNearbyGroups: distance between destinations in meters(?): " + results[0] + ", " + results[1] + ", " + results[2]);
                if (results[0] < DEFAULT_SEARCH_RADIUS_METERS) {
                    Log.d(TAG, "displayNearbyGroups: INSIDE CIRCLE: " + results[0]);
                    // Display this result on map.
                    displayGroup(group);
                } else {
                    Log.d(TAG, "displayNearbyGroups: OUTSIDE CIRCLE: " + results[0]);
                }
            }
        } else {
            Toast.makeText(getActivity(), "Please enter valid path.", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayGroup(Group group) {
        // Display this group on the map
        // Display clickable markers
        // On marker click, display route // TODO: make markers clickable, on click, call joinGroup
        // TODO: put this in the right place!!!
        User user = new User();
        new GetUserAsyncTask(ADD_MEMBER_TO_GROUP, user, null, group, null, new OnTaskComplete() {
            @Override
            public void onSuccess(Object result) {
                if (result ==null) {
                    // FAILED
                }
            }

            @Override
            public void onFailure(Exception e) {

            }
        }).execute();

        // Draw route
        try {
            new DirectionFinder(new DirectionFinderListener() {
                @Override
                public void onDirectionFinderStart() {

                }

                @Override
                public void onDirectionFinderSuccess(List<Route> routes) {
                    polylinePaths = new ArrayList<>();
                    originMarkers = new ArrayList<>();
                    destinationMarkers = new ArrayList<>();

                    for (Route route : routes) {
                        ((TextView) view.findViewById(R.id.tvDuration)).setText(route.duration.text);
                        ((TextView) view.findViewById(R.id.tvDistance)).setText(route.distance.text);

                        originMarkers.add(mMap.addMarker(new MarkerOptions()
                                //.icon(BitmapDescriptorFactory.fromResource(R.drawable.start_blue)) // TODO: SET CUSTOM MARKER
                                .title(route.startAddress)
                                .position(route.startLocation)));
                        destinationMarkers.add(mMap.addMarker(new MarkerOptions()
                                //.icon(BitmapDescriptorFactory.fromResource(R.drawable.end_green)) // TODO: SET CUSTOM MARKER
                                .title(route.endAddress)
                                .position(route.endLocation)));

                        PolylineOptions polylineOptions = new PolylineOptions().
                                geodesic(true).
                                color(getResources().getColor(R.color.logoBlue)).
                                width(10);

                        for (int i = 0; i < route.points.size(); i++)
                            polylineOptions.add(route.points.get(i));

                        polylinePaths.add(mMap.addPolyline(polylineOptions));
                    }
                }
            },
                    group.getRouteLatArray()[0] + ", " + group.getRouteLngArray()[0],
                    group.getRouteLatArray()[1] + ", " + group.getRouteLngArray()[1])
                    .execute();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void joinGroup(Group group) {
        // Add group to user's group list
        // Transition to group window
    }
}
