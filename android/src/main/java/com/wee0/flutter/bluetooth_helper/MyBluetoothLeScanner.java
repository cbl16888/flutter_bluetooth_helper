package com.wee0.flutter.bluetooth_helper;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.ParcelUuid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 自定义的蓝牙扫描器
 */
final class MyBluetoothLeScanner {

    // 最大扫描超时时间：5秒。
    static final int DEF_MAX_SCAN_TIMEOUT = 5000;

    private final Map<String, Map<String, String>> scanData = new HashMap<>(16, 1.0f);

    private BluetoothLeScanner scanner;
    private volatile boolean scanning;
    // 回调注册时间，用于超时处理
    private long _callbackRegTime = 0;

    // 设备名称
    private String _deviceAddress = null;
    // 设备地址
    private String _deviceName = null;
    // 扫描超时时间
    private int _scanTimeout = DEF_MAX_SCAN_TIMEOUT;
    // 响应对象
    private IReply _reply = null;

    // 扫描结果回调
    private final ScanCallback _scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            MyLog.debug("onScanResult. callbackType: {}, result: {}", callbackType, result);
            BluetoothDevice _device = result.getDevice();
            if (null == _device) return;
            String _name = _device.getName();
            String _address = _device.getAddress();
            Map<String, String> _deviceInfo = new HashMap<>(8);
            _deviceInfo.put("deviceId", _address);
            _deviceInfo.put("deviceName", _name);
            if (!scanData.containsKey(_address))
                scanData.put(_address, _deviceInfo);
            if (null != MyBluetoothLeScanner.this._deviceAddress && MyBluetoothLeScanner.this._deviceAddress.equals(_address)) {
                MyLog.debug("scanned device id: {}", _address);
                stopScan();
                return;
            }
            if (null != MyBluetoothLeScanner.this._deviceName && MyBluetoothLeScanner.this._deviceName.equals(_name)) {
                MyLog.debug("scanned device name: {}", _name);
                stopScan();
                return;
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            MyLog.debug("onBatchScanResults. results: {}", results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            MyLog.debug("onScanFailed. errorCode: {}", errorCode);
        }

    };

    void setBluetoothLeScanner(BluetoothLeScanner scanner) {
        this.scanner = scanner;
    }

    BluetoothLeScanner getBluetoothLeScanner() {
        return this.scanner;
    }

    Map<String, Map<String, String>> getLastScanData() {
        return this.scanData;
    }

    void startScan(final String deviceName, final String deviceAddress, final int scanTimeout, final IReply reply, final String serviceId) {
        if (this.scanning) {
            MyLog.debug("already scanning!");
            return;
        }

        this._deviceName = deviceName;
        this._deviceAddress = deviceAddress;
        this._scanTimeout = scanTimeout > 0 ? scanTimeout * 1000 : DEF_MAX_SCAN_TIMEOUT;
        this._reply = reply;

        // 清除之前的扫描结果
        this.scanData.clear();

        if (0 == this._callbackRegTime) {
            if (!MyBluetoothManager.me().isEnabled())
                throw new MyBluetoothException(MyBluetoothException.CODE_BLUETOOTH_NOT_ENABLE, "please turn on bluetooth.");
            if (!MyLocationManager.me.isEnabled(true))
                throw new MyBluetoothException(MyBluetoothException.CODE_LOCATION_NOT_ENABLE, "please turn on location.");
            if (PermissionHelper.me().requestPermission(PermissionHelper.ACCESS_FINE_LOCATION, new ICallback() {
                @Override
                public void execute(Object args) {
                    if (!(Boolean) args) {
                        MyLog.warn("requires location permissions for scanning.");
                        if (null != reply)
                            reply.error(MyBluetoothException.CODE_LOCATION_NOT_GRANTED, "requires location permissions for scanning.");
                        return;
                    }
                    if (System.currentTimeMillis() - _callbackRegTime > 5000) {
                        MyLog.debug("callback request timeout.");
                        return;
                    }
                    startScan(deviceName, deviceAddress, MyBluetoothLeScanner.this._scanTimeout, reply, serviceId);
                }
            })) {
                _callbackRegTime = System.currentTimeMillis();
                MyLog.debug("request location permission...");
                return;
            }
        }

        this.scanning = true;
        MyLog.debug("start scan.");
        final List<ScanFilter> _scanFilters = new ArrayList<>();
        ScanFilter.Builder _filterBuilder = new ScanFilter.Builder();
        if (null != deviceName) _filterBuilder.setDeviceName(deviceName);
        if (null != deviceAddress) _filterBuilder.setDeviceAddress(deviceAddress);
        if (null != serviceId) {
            _filterBuilder.setServiceUuid(ParcelUuid.fromString(serviceId));
        }

        ScanFilter _scanFilter = _filterBuilder.build();
        _scanFilters.add(_scanFilter);

        ScanSettings.Builder _settingsBuilder = new ScanSettings.Builder();
        _settingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
//        _settingsBuilder.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE);
//        _settingsBuilder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
//        _settingsBuilder.setLegacy(true);
        final ScanSettings _scanSettings = _settingsBuilder.build();
        this.scanner.startScan(_scanFilters, _scanSettings, _scanCallback);

        MyHandler.me().delayed(MyHandler.ID_SCAN_TIMEOUT, this._scanTimeout, new ICallback() {
            @Override
            public void execute(Object args) {
                MyLog.debug("scan timeout, to be stop.");
                stopScan();
            }
        });

//        reply.success(true);
    }

    /**
     * 停止扫描，返回扫描结果
     *
     * @return 扫描结果
     */
    Map<String, Map<String, String>> stopScan() {
        MyLog.debug("stop scan. data: {}", this.scanData);
        this._callbackRegTime = 0;
        this.scanning = false;
        MyHandler.me().removeCallback(MyHandler.ID_SCAN_TIMEOUT);
        if (null != this.scanner) {
            try {
                this.scanner.stopScan(_scanCallback);
            } catch (IllegalStateException e) {
                MyLog.debug("stopScan error: {}", e.getMessage());
            }
        }
        if (null != this._reply) {
            this._reply.success(this.scanData);
            this._reply = null;
        }
        return this.scanData;
    }

}
