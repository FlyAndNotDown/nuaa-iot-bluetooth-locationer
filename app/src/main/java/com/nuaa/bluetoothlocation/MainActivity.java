package com.nuaa.bluetoothlocation;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.scan.BleScanRuleConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    // 允许的蓝牙设备MAC及编号
    private HashMap<String, Integer> allowBluetoothDeviceMacs = new HashMap<String, Integer>(){{
        // 初始化允许的蓝牙设备
        put("31:DD:8C:8B:DB:71", 0);
        put("37:E5:69:B1:B1:CA", 1);
        put("6A:51:F7:AF:44:5B", 2);
    }};
    // 蓝牙设备对象
    private ArrayList<BleDevice> bluetoothDevices = new ArrayList<BleDevice>() {{
        add(null);
        add(null);
        add(null);
    }};
    // 设备就绪状态
    private boolean [] bluetoothReadyStates = new boolean[] {false, false, false};

    // 尝试增加的距离步长
    private static final double TRY_DISTANCE_STEP = 0.01;

    private TextView bleDevice1Ready;
    private TextView bleDevice1Rssi;
    private TextView bleDevice2Ready;
    private TextView bleDevice2Rssi;
    private TextView bleDevice3Ready;
    private TextView bleDevice3Rssi;
    private EditText roomX;
    private EditText roomY;
    private Button refreshButton;
    private Button calculateButton;
    private TextView location;

    private void scan() {
        bluetoothDevices.clear();
        for (int i = 0; i < 3; i++) bluetoothDevices.add(null);
        // 配置扫描规则
        BleManager.getInstance()
                .initScanRule(new BleScanRuleConfig.Builder()
                        .setAutoConnect(false)
                        .setScanTimeOut(10000)
                        .build()
                );
        // 打开蓝牙
        BleManager.getInstance().enableBluetooth();
        // 开始扫描
        BleManager.getInstance().scan(new BleScanCallback() {
            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                Toast.makeText(getApplicationContext(), "扫描完成!", Toast.LENGTH_SHORT)
                        .show();
            }

            @Override
            public void onScanStarted(boolean success) {}

            @Override
            public void onScanning(BleDevice bleDevice) {
                // 如果扫描到了一个新设备
                // 看他是不是预先设定的那几个设备
                for (String mac : allowBluetoothDeviceMacs.keySet()) {
                    // 如果是
                    if (mac.equals(bleDevice.getMac())) {
                        // 获取index
                        int index = allowBluetoothDeviceMacs.get(mac);
                        // 将设备加入list中
                        bluetoothDevices.remove(index);
                        bluetoothDevices.add(index, bleDevice);

                        // 更新显示状态
                        bluetoothReadyStates[index] = true;
                        switch (index) {
                            case 0:
                                bleDevice1Ready.setText("就绪");
                                bleDevice1Rssi.setText(String.valueOf(bleDevice.getRssi()));
                                break;
                            case 1:
                                bleDevice2Ready.setText("就绪");
                                bleDevice2Rssi.setText(String.valueOf(bleDevice.getRssi()));
                                break;
                            case 2:
                                bleDevice3Ready.setText("就绪");
                                bleDevice3Rssi.setText(String.valueOf(bleDevice.getRssi()));
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        });
    }

    private void bindComponent() {
        // 绑定所有组件
        bleDevice1Ready = findViewById(R.id.bleDevice1_ready);
        bleDevice1Rssi = findViewById(R.id.bleDevice1_rssi);
        bleDevice2Ready = findViewById(R.id.bleDevice2_ready);
        bleDevice2Rssi = findViewById(R.id.bleDevice2_rssi);
        bleDevice3Ready = findViewById(R.id.bleDevice3_ready);
        bleDevice3Rssi = findViewById(R.id.bleDevice3_rssi);
        roomX = findViewById(R.id.room_x);
        roomY = findViewById(R.id.room_y);
        refreshButton = findViewById(R.id.refresh_button);
        calculateButton = findViewById(R.id.calculate_button);
        location = findViewById(R.id.location);

        // 设置回调
        // 刷新状态按钮回调
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 更改显示状态为默认状态
                for (int i = 0; i < 2; i++) {
                    bluetoothReadyStates[i] = false;
                }
                bleDevice1Ready.setText("未就绪");
                bleDevice1Rssi.setText("0");
                bleDevice2Ready.setText("未就绪");
                bleDevice2Rssi.setText("0");
                bleDevice3Ready.setText("未就绪");
                bleDevice3Rssi.setText("0");
                // 开始一轮新的扫描
                scan();
            }
        });
        // 计算按钮回调
        calculateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 先判断三个设备是否都已经准备就绪
                for (int i = 0; i < 2; i++) {
                    if (!bluetoothReadyStates[i]) {
                        Toast.makeText(getApplicationContext(), "设备" + i + "未就绪，无法启动计算",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                // 如果全部准备就绪了，则开始计算
                // 定义三个设备的坐标
                MathTool.Point device1Point = new MathTool.Point(0, 0);
                MathTool.Point device2Point = new MathTool.Point(Double.parseDouble(roomX.getText().toString()), 0);
                MathTool.Point device3Point = new MathTool.Point(0, Double.parseDouble(roomY.getText().toString()));

                // 将三个rssi都转换成实际的距离
                double [] distances = new double[3];
                for (int i = 0; i < 3; i++) {
                    distances[i] = MathTool.rssiToDistance(bluetoothDevices.get(i).getRssi()) * Math.cos(Math.toRadians(45));
                }

                // 抽象圆
                MathTool.Circle circle1 = new MathTool.Circle(
                        new MathTool.Point(device1Point.x, device1Point.y),
                        distances[0]);
                MathTool.Circle circle2 = new MathTool.Circle(
                        new MathTool.Point(device2Point.x, device2Point.y),
                        distances[1]
                );
                MathTool.Circle circle3 = new MathTool.Circle(
                        new MathTool.Point(device3Point.x, device3Point.y),
                        distances[2]
                );
                // 尝试进行运算
                while (true) {
                    // 先看三个圆之间是否各自都有交点
                    // 如果1、2两个圆之间没有交点
                    if (!MathTool.isTwoCircleIntersect(circle1, circle2)) {
                        // 尝试增加某个圆的半径，谁半径更大增加谁的
                        if (circle1.r > circle2.r) {
                            circle1.r += TRY_DISTANCE_STEP;
                        } else {
                            circle2.r += TRY_DISTANCE_STEP;
                        }
                        continue;
                    }
                    // 如果1、3两个圆之间没有交点
                    if (!MathTool.isTwoCircleIntersect(circle1, circle3)) {
                        // 尝试增加半径
                        // 如果c3的半径比两者之中任意一个都小
                        if (circle3.r < circle1.r && circle3.r < circle2.r) {
                            circle1.r += TRY_DISTANCE_STEP;
                            circle2.r += TRY_DISTANCE_STEP;
                        } else {
                            circle3.r += TRY_DISTANCE_STEP;
                        }
                        continue;
                    }
                    // 如果2、3两个原之间没有交点
                    if (!MathTool.isTwoCircleIntersect(circle2, circle3)) {
                        // 尝试增加半径
                        // 如果c3的半径比两者之中任意一个都小
                        if (circle3.r < circle1.r && circle3.r < circle2.r) {
                            circle1.r += TRY_DISTANCE_STEP;
                            circle2.r += TRY_DISTANCE_STEP;
                        } else {
                            circle3.r += TRY_DISTANCE_STEP;
                        }
                        continue;
                    }

                    // 等尝试到三个圆都有交点的时候，求出各自两个圆之间的交点
                    MathTool.PointVector2 temp1 = MathTool.getIntersectionPointsOfTwoIntersectCircle(circle1, circle2);
                    MathTool.PointVector2 temp2 = MathTool.getIntersectionPointsOfTwoIntersectCircle(circle2, circle3);
                    MathTool.PointVector2 temp3 = MathTool.getIntersectionPointsOfTwoIntersectCircle(circle3, circle1);
                    // 1、2两圆的交点取y > 0 的那个点
                    MathTool.Point resultPoint1 = temp1.p1.y > 0 ?
                            new MathTool.Point(temp1.p1.x, temp1.p1.y) :
                            new MathTool.Point(temp1.p2.x, temp1.p2.y);
                    // 2、3两圆的交点取两者的均值
                    MathTool.Point resultPoint2 = new MathTool.Point(
                            (temp2.p1.x + temp2.p2.x) / 2,
                            (temp2.p1.y + temp2.p2.y) / 2
                    );
                    // 3、1两圆的交点取x > 0的那个点
                    MathTool.Point resultPoint3 = temp3.p1.x > 0 ?
                            new MathTool.Point(temp3.p1.x, temp3.p1.y) :
                            new MathTool.Point(temp3.p2.x, temp3.p2.y);

                    // 求出三个点的中心点
                    MathTool.Point resultPoint = MathTool.getCenterOfThreePoint(
                            resultPoint1,
                            resultPoint2,
                            resultPoint3
                    );

                    // 更新结果显示
                    location.setText(resultPoint.toString());

                    // 跳出循环
                    break;
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ble初始化
        BleManager.getInstance()
                .init(getApplication());

        // 绑定组件
        bindComponent();
    }
}
