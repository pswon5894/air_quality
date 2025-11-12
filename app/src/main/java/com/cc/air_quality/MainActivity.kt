package com.cc.air_quality

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cc.air_quality.databinding.ActivityMainBinding
import com.cc.air_quality.retrofit.AirQualityService
import com.cc.air_quality.retrofit.RetrofitConnection
import java.io.IOException
import java.util.Locale

class MainActivity : AppCompatActivity() {

    lateinit var binding : ActivityMainBinding
    lateinit var locationProvider: LocationProvider

    private val PERMISSIONS_REQUEST_CODE = 100

    val REQUIRED_PERMISSIONS =arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    lateinit var getGPSPermissionLauncher : ActivityResultLauncher<Intent>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)

        checkAllPermissions()
        updateUI()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun updateUI() {
        locationProvider = LocationProvider(this@MainActivity)

        val latitude: Double? = locationProvider.getLocationLatitude()
        val longitude : Double? = locationProvider.getLocationLongitude()

        if(latitude != null && longitude != null) {
            //1. 현재 위치 가져오고 UI 업데이트
            val address = getCurrentAddress(latitude, longitude)

            address?.let{
                binding.tvLocationTitle.text="${it.subLocality}"
                //도로명
                binding.tvLocationSubtitle.text="${it.countryName} ${it.adminArea}"
            }
            //2. 미세먼지 농도 가져오고 UI 업데이트

            getAirQualityData(latitude, longitude)
        }else{
            Toast.makeText(this, "위도, 경도 정보를 가져올 수 없습니다.", Toast.LENGTH_LONG).show()
        }
    }

    private fun getAirQualityData(latitude: Double, longitude: Double){
        var retrofitAPI = RetrofitConnection.getInstace().create(
            AirQualityService::class.java
        )

        retrofitAPI.getAirQualityData(
            latitude.toString(),
            longitude.toString(),
            apiKey
        )
    }

    private fun getCurrentAddress (latitude : Double, longitude : Double) : Address?{
        val geoCoder = Geocoder(this, Locale.KOREA)
        //getDefault() or KOREA
        val addresses: List<Address>?

        addresses = try {
            geoCoder.getFromLocation(latitude, longitude, 7)
        }catch (ioException : IOException){
            Toast.makeText(this, "지오코더 서비스를 이용불가 합니다", Toast.LENGTH_LONG).show()
            return null
        }catch (illegalArgumentException : java.lang.IllegalArgumentException){
            Toast.makeText(this, "잘못된 위도, 경도입니다.", Toast.LENGTH_LONG).show()
            return null
        }

        if(addresses == null || addresses.size == 0) {
            Toast.makeText(this, "주소가 발견되지 않았습니다.", Toast.LENGTH_LONG).show()
            return null
        }

        return addresses[0]
    }

    private fun checkAllPermissions() {
        if(!isLocationServiceAvailable()){
            showDialogForLocaionServiceSetting()
        } else{
            isRunTimePermissionsGranted()
        }
    }

    private fun isLocationServiceAvailable() :Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER))
    }

    private fun isRunTimePermissionsGranted(){
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION)
        val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION)

        if(hasFineLocationPermission != PackageManager.PERMISSION_GRANTED || hasCoarseLocationPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this@MainActivity,REQUIRED_PERMISSIONS,PERMISSIONS_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == PERMISSIONS_REQUEST_CODE && grantResults.size == REQUIRED_PERMISSIONS.size){
            var checkResult = true

            for (result in grantResults){
                if(result != PackageManager.PERMISSION_GRANTED){
                    checkResult =false
                    break;
                }
            }

            if(checkResult) {
                // 위치값을 가져올 수 있음
                updateUI()
            } else {
                Toast.makeText(this@MainActivity, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼시션을 허용해주세요.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun showDialogForLocaionServiceSetting() {
        getGPSPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {result ->
            if(result.resultCode == Activity.RESULT_OK) {
                if(isLocationServiceAvailable()) {
                    isRunTimePermissionsGranted()
                } else{
                    Toast.makeText(this@MainActivity, "위치 서비스를 사용할 수 없습니다.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
        val builder : AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("위치 서비스 비활성화")
        builder.setMessage("위치 서비스가 꺼져있습니다. 설정해야 앱을 사용할 수 있습니다.")
        builder.setCancelable(true)
        builder.setPositiveButton("설정", DialogInterface.OnClickListener { dialogInterface, i ->
            val callGPSSettingIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            getGPSPermissionLauncher.launch(callGPSSettingIntent)
        })
        builder.setNegativeButton("취소", DialogInterface.OnClickListener {dialogInterface, i ->
            dialogInterface.cancel()
            Toast.makeText(this@MainActivity, "위치 서비스를 사용할 수 없습니다.", Toast.LENGTH_LONG).show()
            finish()
        })
        builder.create().show()
    }
}

//ContextCompat, ActivityCompat : 권한 요청/확인 도우미
//ActivityResultLauncher : 다른 화면을 열고 결과를 받을 때 사용

//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
//    }
//화면 상단/하단 시스템바(상태바, 네비게이션바) 때문에 레이아웃이 가려지지 않도록 여백을 자동으로 조정합니다.

//private fun printAddressInfo(address: Address) {
//    Log.d("AddressInfo", """
//        countryName: ${address.countryName}
//        adminArea: ${address.adminArea}
//        subAdminArea: ${address.subAdminArea}
//        locality: ${address.locality}
//        subLocality: ${address.subLocality}
//        thoroughfare: ${address.thoroughfare}
//        subThoroughfare: ${address.subThoroughfare}
//        featureName: ${address.featureName}
//        postalCode: ${address.postalCode}
//        latitude: ${address.latitude}
//        longitude: ${address.longitude}
//    """.trimIndent())
//}

//지오코딩 23년 이후로 도로명 제대로 작동하지 않음 도로명은 다른 방법 api 추천