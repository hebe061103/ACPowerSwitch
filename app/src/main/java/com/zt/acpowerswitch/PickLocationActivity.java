package com.zt.acpowerswitch;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;

import java.util.ArrayList;
import java.util.List;

public class PickLocationActivity extends AppCompatActivity
        implements AMap.OnCameraChangeListener, GeocodeSearch.OnGeocodeSearchListener {

    private MapView mapView;
    private AMap aMap;
    private GeocodeSearch geocodeSearch;
    private AMapLocationClient locationClient;

    private double pickLat, pickLng;
    private String pickCity = "";
    private String pickAddress = "";

    private boolean isFirstLocated = false; // 防止反复移镜头

    private static final int LOC_PERMISSION_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 高德隐私合规（必须最前）
        MapsInitializer.updatePrivacyShow(this, true, true);
        MapsInitializer.updatePrivacyAgree(this, true);

        setContentView(R.layout.activity_pick_location);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        aMap = mapView.getMap();
        aMap.setMapType(AMap.MAP_TYPE_NORMAL);

        // 定位蓝点样式：只定位一次，不跟随
        MyLocationStyle style = new MyLocationStyle();
        style.myLocationType(MyLocationStyle.LOCATION_TYPE_SHOW);
        aMap.setMyLocationStyle(style);
        aMap.setMyLocationEnabled(true);

        aMap.moveCamera(CameraUpdateFactory.zoomTo(15));
        aMap.setOnCameraChangeListener(this);

        try {
            geocodeSearch = new GeocodeSearch(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        geocodeSearch.setOnGeocodeSearchListener(this);

        // 默认先放北京（定位失败时的兜底）
        LatLng beijing = new LatLng(39.90469, 116.40717);
        pickLat = beijing.latitude;
        pickLng = beijing.longitude;
        reverseGeo(beijing);

        // 开始定位
        checkLocationPermissionThenLocate();

        // 确定按钮
        findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            Intent data = new Intent();
            data.putExtra("lat", pickLat);
            data.putExtra("lon", pickLng);
            data.putExtra("city", pickCity);
            data.putExtra("address", pickAddress);
            setResult(RESULT_OK, data);
            finish();
        });
    }

    /* ===================== 定位 ===================== */

    private void checkLocationPermissionThenLocate() {
        List<String> need = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            need.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            need.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (!need.isEmpty()) {
            ActivityCompat.requestPermissions(this, need.toArray(new String[0]), LOC_PERMISSION_CODE);
        } else {
            startSingleLocation();
        }
    }

    private void startSingleLocation() {
        try {
            locationClient = new AMapLocationClient(getApplicationContext());
            AMapLocationClientOption opt = new AMapLocationClientOption();
            opt.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            opt.setOnceLocation(true);          // 单次定位
            opt.setNeedAddress(true);          // 带回城市信息
            opt.setHttpTimeOut(15000);
            locationClient.setLocationOption(opt);

            locationClient.setLocationListener(loc -> {
                if (loc != null && loc.getErrorCode() == 0) {
                    double lat = loc.getLatitude();
                    double lng = loc.getLongitude();

                    LatLng me = new LatLng(lat, lng);
                    if (!isFirstLocated) {
                        // 第一次定位：把镜头移过去
                        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(me, 16));
                        isFirstLocated = true;
                    }
                    pickLat = lat;
                    pickLng = lng;
                    pickCity = loc.getCity() == null ? loc.getProvince() : loc.getCity();
                    reverseGeo(me);
                } else {
                    Toast.makeText(PickLocationActivity.this,
                            "定位失败，请手动拖动地图选点", Toast.LENGTH_SHORT).show();
                }
                if (locationClient != null) locationClient.stopLocation();
            });

            locationClient.startLocation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOC_PERMISSION_CODE) {
            boolean ok = true;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) {
                    ok = false;
                    break;
                }
            }
            if (ok) startSingleLocation();
            else Toast.makeText(this, "未授予定位权限，可手动选点", Toast.LENGTH_SHORT).show();
        }
    }

    /* ===================== 地图拖动 ===================== */

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {}

    @Override
    public void onCameraChangeFinish(CameraPosition cameraPosition) {
        reverseGeo(cameraPosition.target);
    }

    private void reverseGeo(LatLng latLng) {
        pickLat = latLng.latitude;
        pickLng = latLng.longitude;

        RegeocodeQuery query = new RegeocodeQuery(
                new LatLonPoint(latLng.latitude, latLng.longitude),
                100,
                GeocodeSearch.AMAP
        );
        geocodeSearch.getFromLocationAsyn(query);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onRegeocodeSearched(RegeocodeResult result, int code) {
        if (code == AMapException.CODE_AMAP_SUCCESS && result != null) {
            RegeocodeAddress addr = result.getRegeocodeAddress();
            if (addr != null) {
                pickCity = addr.getCity() == null ? addr.getProvince() : addr.getCity();
                pickAddress = addr.getFormatAddress();
                TextView tv = findViewById(R.id.tvCity);
                if (tv != null) tv.setText(pickCity + "\n" + pickAddress);
            }
        }
    }

    @Override
    public void onGeocodeSearched(GeocodeResult result, int code) {}

    /* ===================== 生命周期 ===================== */

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationClient != null) locationClient.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}