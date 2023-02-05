package io.sellmair.broadheart

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.pm.PackageManager
import android.os.ParcelUuid
import androidx.core.app.ActivityCompat
import java.util.UUID

fun broadcast() {
    /*
    val uuid = UUID.randomUUID()
    val adapter = BluetoothAdapter.getDefaultAdapter()
    val settings = AdvertiseSettings.Builder().build()
    val data = "hello".encodeToByteArray()
    val advertiseData = AdvertiseData.Builder()
        .addServiceData(ParcelUuid(uuid), data)
        .build()
    val advertiser = adapter.bluetoothLeAdvertiser
    if (ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_ADVERTISE
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        // TODO: Consider calling
        //    ActivityCompat#requestPermissions
        // here to request the missing permissions, and then overriding
        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
        //                                          int[] grantResults)
        // to handle the case where the user grants the permission. See the documentation
        // for ActivityCompat#requestPermissions for more details.
        return
    }
    advertiser.startAdvertising(settings, advertiseData, object : AdvertiseCallback() {

    })
    advertiser.startAdvertisingSet()

     */
}