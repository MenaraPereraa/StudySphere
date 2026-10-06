package lk.menara.studysphere.activity;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lk.menara.studysphere.R;

public class MapPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapPickerActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private static final double DEFAULT_LAT = 6.9137837;
    private static final double DEFAULT_LNG = 79.9240185;
    private static final int CAMERA_DURATION_MS = 1500;
    private static final int ROUTE_DURATION_MS = 1200;
    private static final float CLOSE_ZOOM = 18f;
    private static final float MIN_ROUTE_DISTANCE_M = 30f;

    private GoogleMap googleMap;
    private LatLng selectedLatLng = null;
    private LatLng originLatLng = null;
    private FusedLocationProviderClient fusedLocationClient;
    private SearchView searchView;
    private Button btnSelectLocation;
    private Polyline routePolyline;
    private ValueAnimator routeAnimator;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);

        searchView = findViewById(R.id.search_location);
        btnSelectLocation = findViewById(R.id.btnSelectLocation);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    if (query != null && !query.trim().isEmpty()) {
                        searchLocation(query.trim());
                    }
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    return false;
                }
            });
        }

        if (btnSelectLocation != null) {
            btnSelectLocation.setOnClickListener(v -> {
                if (selectedLatLng != null) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("lat", selectedLatLng.latitude);
                    resultIntent.putExtra("lng", selectedLatLng.longitude);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } else {
                    Toast.makeText(this, "Please select a location on the map", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        applyCustomMapStyle();

        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);

        getCurrentLocation();

        googleMap.setOnMapClickListener(this::updateMarker);
    }

    private void applyCustomMapStyle() {
        try {
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark));
            if (!success) {
                Log.e(TAG, "Map style parsing failed");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Map style resource not found", e);
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        googleMap.setMyLocationEnabled(true);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        originLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    } else {
                        originLatLng = new LatLng(DEFAULT_LAT, DEFAULT_LNG);
                    }
                    updateMarker(originLatLng);
                })
                .addOnFailureListener(this, e -> {
                    originLatLng = new LatLng(DEFAULT_LAT, DEFAULT_LNG);
                    updateMarker(originLatLng);
                });
    }

    private void searchLocation(String locationName) {
        if (!Geocoder.isPresent()) {
            Toast.makeText(this, "Search is not available on this device", Toast.LENGTH_SHORT).show();
            return;
        }

        executor.execute(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addressList = geocoder.getFromLocationName(locationName, 1);
                runOnUiThread(() -> {
                    if (isDestroyed() || isFinishing()) return;
                    if (addressList != null && !addressList.isEmpty()) {
                        Address address = addressList.get(0);
                        updateMarker(new LatLng(address.getLatitude(), address.getLongitude()));
                    } else {
                        Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                Log.e(TAG, "Geocoder error", e);
                runOnUiThread(() -> {
                    if (isDestroyed() || isFinishing()) return;
                    Toast.makeText(this, "Error searching location", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateMarker(LatLng latLng) {
        if (googleMap == null) return;

        if (routeAnimator != null) {
            routeAnimator.cancel();
            routeAnimator = null;
        }

        googleMap.clear();
        routePolyline = null;
        selectedLatLng = latLng;

        boolean hasRoute = originLatLng != null && distanceBetween(originLatLng, latLng) > MIN_ROUTE_DISTANCE_M;

        if (hasRoute) {
            googleMap.addMarker(new MarkerOptions()
                    .position(originLatLng)
                    .title("Start")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        }

        googleMap.addMarker(new MarkerOptions().position(latLng).title("Selected Location"));

        if (hasRoute) {
            drawRoute(originLatLng, latLng);
            animateCameraToBounds(originLatLng, latLng);
        } else {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, CLOSE_ZOOM), CAMERA_DURATION_MS, null);
        }
    }

    private void drawRoute(LatLng from, LatLng to) {
        final Polyline polyline = googleMap.addPolyline(new PolylineOptions()
                .add(from, from)
                .color(Color.parseColor("#4DD0E1"))
                .width(12f)
                .geodesic(true)
                .startCap(new RoundCap())
                .endCap(new RoundCap()));
        routePolyline = polyline;

        routeAnimator = ValueAnimator.ofFloat(0f, 1f);
        routeAnimator.setDuration(ROUTE_DURATION_MS);
        routeAnimator.setInterpolator(new LinearInterpolator());
        routeAnimator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();
            double lat = from.latitude + (to.latitude - from.latitude) * fraction;
            double lng = from.longitude + (to.longitude - from.longitude) * fraction;
            polyline.setPoints(Arrays.asList(from, new LatLng(lat, lng)));
        });
        routeAnimator.start();
    }

    private void animateCameraToBounds(LatLng a, LatLng b) {
        LatLngBounds bounds = new LatLngBounds.Builder().include(a).include(b).build();
        try {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 160), CAMERA_DURATION_MS, null);
        } catch (IllegalStateException e) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(b, CLOSE_ZOOM), CAMERA_DURATION_MS, null);
        }
    }

    private float distanceBetween(LatLng a, LatLng b) {
        float[] result = new float[1];
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, result);
        return result[0];
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            boolean granted = false;
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break;
                }
            }
            if (granted) {
                getCurrentLocation();
            } else {
                originLatLng = new LatLng(DEFAULT_LAT, DEFAULT_LNG);
                updateMarker(originLatLng);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (routeAnimator != null) {
            routeAnimator.cancel();
        }
        executor.shutdownNow();
    }
}