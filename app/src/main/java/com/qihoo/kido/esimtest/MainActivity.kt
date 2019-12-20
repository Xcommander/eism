package com.qihoo.kido.esimtest

import android.bluetooth.BluetoothGatt
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.clj.fastble.data.BleDevice
import com.clj.fastble.exception.BleException
import com.velocate.lpa.v22.core.ConsentData
import com.velocate.lpa.v22.core.EuiccProfile
import com.velocate.lpa.v22.core.LpaEventListener
import com.velocate.lpa.v22.core.LpaManager
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), LpaEventListener {

    lateinit var lpaManager: LpaManager

    var watchList: MutableList<BleDevice>? = null

    var profiles: MutableList<EuiccProfile>? = null


    lateinit var adapter: ProfileAdapter

    val STATE_ENABLE = 100
    val STATE_DISABLE = 101
    val STATE_DELETE = 102

    var state: Int = STATE_ENABLE
    var bleDeviceName: String? = null
    var bluetoothConnect = false
    var install = false
    //超时处理
    val mHandler: Handler = Handler(Handler.Callback {
        when (it.what) {
            1 -> {
                Toast.makeText(this, getString(R.string.get_eid_timeout), Toast.LENGTH_LONG).show()
                reset()
                return@Callback false
            }

            2 -> {
                Toast.makeText(this, getString(R.string.get_profile_list_timeout), Toast.LENGTH_LONG).show()
                reset()
                return@Callback false
            }
            else -> {
                return@Callback false
            }

        }
    })


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkCallingPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                init()
            } else {
                requestPermissions(arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION), 1)
            }
        } else {
            init()
        }

    }

    fun init() {
        initView()
        initData()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            init()
        } else {
            Toast.makeText(this, "", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initView() {
        rv_watch_list.layoutManager = LinearLayoutManager(this)
        adapter = ProfileAdapter(this)
        adapter.itemClick = object : ProfileAdapter.ItemClick {
            override fun onClick(positon: Int) {
                cl_devices_list.visibility = View.GONE
                setTip(getString(R.string.ble_prepare))
                BleFastManager.bleDevice = watchList?.get(positon)
                bleDeviceName = BleFastManager.bleDevice?.name
                BleFastManager.connect()
            }

        }
        rv_watch_list.adapter = adapter

        bt_start.setOnClickListener {
            BleFastManager.scan()
        }
        iv_refresh.setOnClickListener {
            BleFastManager.scan()
        }
        setTip(getString(R.string.tip_start))
    }

    private fun initData() {
        initLpa()
        initBle()

    }

    /**
     * 初始化LPA esim SDK
     */
    private fun initLpa() {
        lpaManager = LpaManager(this)

    }

    /**
     * 初始化BLE 蓝牙
     */
    private fun initBle() {
        BleFastManager.initBle(application)

        BleFastManager.mBleScanListener = object : BleFastManager.BleScanListener {

            override fun onScaning(bleDevice: BleDevice?) {
                Toast.makeText(this@MainActivity, "正在搜索设备", Toast.LENGTH_SHORT).show()
                //BleFastManager.bleDevice = bleDevice
            }

            override fun onScanStarted(success: Boolean) {
                Toast.makeText(this@MainActivity, "开始搜索", Toast.LENGTH_SHORT).show()
            }

            override fun onScanFinish(scanResultList: MutableList<BleDevice>?) {
                Toast.makeText(this@MainActivity, "搜索完成", Toast.LENGTH_SHORT).show()
                if (state == STATE_ENABLE) {
                    bt_start.visibility = View.GONE
                    cl_devices_list.visibility = View.VISIBLE
                    setTip(getString(R.string.tip_enable))
                    if (scanResultList.isNullOrEmpty()) {
                        tv_empty.visibility = View.VISIBLE
                        rv_watch_list.visibility = View.GONE
                        tv_empty.text = getString(R.string.ble_list_null)
                    } else {
                        tv_empty.visibility = View.GONE
                        rv_watch_list.visibility = View.VISIBLE
                        watchList = scanResultList
                        refreshData()
                    }
                } else {
                    cl_devices_list.visibility = View.GONE
                    //这边应该重新去连接下
                    if (scanResultList != null) {
                        for (bleDevice in scanResultList) {
                            if (bleDevice.name == this@MainActivity.bleDeviceName) {
                                BleFastManager.bleDevice = bleDevice
                                bluetoothConnect = true
                                BleFastManager.connect()
                                break
                            }
                        }
                    }
                    if (!bluetoothConnect) {
                        Toast.makeText(this@MainActivity, getString(R.string.error_ble), Toast.LENGTH_LONG).show()
                    }

                }
                bluetoothConnect = false
            }


        }

        BleFastManager.mBleNotifyListener = object : BleFastManager.BleNotifyListener {
            override fun onCharacteristicChanged(data: ByteArray) {
                lpaManager.processResponse(data)
            }

            override fun onNotifySuccess() {
            }

            override fun onNotifyFailure(exception: BleException?) {
            }
        }
        BleFastManager.mBleWriteListener = object : BleFastManager.BleWriteListener {
            override fun writeSuccess(current: Int, total: Int, justWrite: ByteArray) {

            }

            override fun writeFailed(exception: BleException?) {
            }

        }
        BleFastManager.mBleConnectListener = object : BleFastManager.BleConnectListener {
            override fun onStartConnect() {
                Toast.makeText(this@MainActivity, "开始连接", Toast.LENGTH_SHORT).show()
            }

            override fun onDisConnected(
                    isActiveDisConnected: Boolean,
                    device: BleDevice?,
                    gatt: BluetoothGatt?,
                    status: Int
            ) {
                Toast.makeText(this@MainActivity, "与设备失去连接", Toast.LENGTH_SHORT).show()
                bluetoothConnect = false
            }

            override fun onConnectSuccess(bleDevice: BleDevice?, gatt: BluetoothGatt?, status: Int) {
                Toast.makeText(this@MainActivity, "连接成功", Toast.LENGTH_SHORT).show()
                bluetoothConnect = true
                mHandler.sendEmptyMessageDelayed(1, 10 * 1000)
                lpaManager.getEid()
            }

            override fun onConnectFail(bleDevice: BleDevice?, exception: BleException?) {
                Toast.makeText(this@MainActivity, "连接失败", Toast.LENGTH_SHORT).show()
            }

        }
    }


    fun refreshData() {
        adapter.setData(watchList)
    }

    private fun getprofilesList() {
        mHandler.sendEmptyMessageDelayed(2, 60 * 1000)
        lpaManager.getProfileList()
    }

    private fun setTip(tip: String) {
        tv_tips.text = tip
    }

    override fun onInstallationConsentRequired(p0: ConsentData?) {
    }

    override fun onEuiccAddressesRetrieved(p0: String?, p1: String?) {
    }

    override fun onProfileNicknameUpdResult(p0: String?, p1: Boolean) {
    }

    override fun onEidRetrieved(p0: String?) {
        //这里应该是get list
        mHandler.removeMessages(1)
        getprofilesList()
    }

    override fun onCommandReady(p0: ByteArray) {
        Log.d("EsimDownloadActivity", "onCommandReady=" + p0.toString(Charsets.UTF_8))
        if (p0.isNotEmpty()) {
            BleFastManager.write(p0)
        }
    }

    override fun onProfileListRetrieved(p0: MutableList<EuiccProfile>?) {
        mHandler.removeMessages(2)
        profiles = p0
        var profile: EuiccProfile? = null
        if (!profiles.isNullOrEmpty()) {
            profile = profiles!![0]
        }
        /**
         * (1)profile 为null，则手表没有profile，则下载profile，下载完成后，去enable profile
         * (2)profile 不为null，state为enable，则disable profile
         * (3)profile 不为null，state为disable ,则delete profile
         */
        if (profile == null) {
            //profile 为null，下载profile
            setTip(getString(R.string.installing))
            lpaManager.installProfile(getString(R.string.active_code))

        } else if (profile.status == 0) {
            //profile 状态为disable，(1)刚安装好，则去enable profile (2)是之前安装,则delete profile
            if (install) {
                setTip(getString(R.string.ble_enable))
                install = false
                lpaManager.enableProfile(profile.iccid)
            } else {
                setTip(getString(R.string.ble_delete))
                lpaManager.deleteProfile(profile.iccid)
            }

        } else if (profile.status == 1) {
            //profile 为enable，disable profile
            setTip(getString(R.string.ble_disable))
            lpaManager.disableProfile(profile.iccid)
        }

    }

    override fun onEuiccInfoRetrieved() {
    }

    override fun onProfileDisableResult(p0: String?, p1: Boolean) {
        if (p1) {
            bt_start.visibility = View.VISIBLE
            bt_start.text = getString(R.string.start_test_again)
            state = STATE_DELETE
            setTip(getString(R.string.tip_disable_success))
        }
    }


    override fun onNotificationsProcessed(p0: Boolean) {
        bt_start.visibility = View.VISIBLE
        bt_start.text = getString(R.string.start_test)
        state = STATE_ENABLE
        setTip(getString(R.string.tip_test_success))
        bleDeviceName = null
    }

    private fun reset() {
        bt_start.visibility = View.VISIBLE
        bt_start.text = getString(R.string.start_test)
        state = STATE_ENABLE
        setTip(getString(R.string.tip_start))
        bleDeviceName = null
    }

    override fun onEuiccReset(p0: Boolean) {
    }

    override fun onDefaultSmdpAddressReset(p0: Boolean) {
    }

    override fun onInstallProfileResult(p0: Boolean) {
        if (p0) {
            install = true
            getprofilesList()
        }
    }

    override fun onDefaultSmdpAddressSet(p0: Boolean) {
    }

    override fun onProfileEnableResult(p0: String?, p1: Boolean) {
        if (p1) {
            bt_start.visibility = View.VISIBLE
            bt_start.text = getString(R.string.start_test_again)
            state = STATE_DISABLE
            setTip(getString(R.string.tip_enable_success))
        }
    }

    override fun onProfileDeleteResult(p0: String?, p1: Boolean) {
        if (p1) {
            //删除notification
            lpaManager.processNotifications()
        }
    }

    override fun onError(p0: String?) {

    }
}
