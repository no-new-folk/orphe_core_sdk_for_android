# ORPHE CORE SDK for Android

ORPHE COREに接続するためのJava SDKを提供します。
現在のところリアルタイムのセンサー値のみを取得可能です。

## インストール

### ソースそのものを利用する

1. `orphecoresdk`フォルダ内にあるすべてのファイルを利用したいプロジェクトのルートにコピーしてください

2. プロジェクトの`app/build.gradle(.kts)`を開き`dependencies`に下記を追加します。

    ```
    implementation(project(":orphecoresdk"))
    ```

### aarファイルを追加します。

※開発途中でありaarファイルが最新でない場合があります。その場合は上記の**ソースそのものを利用する**でSDKを追加してください。

1. `orphecoresdk-release-xxx.aar`を下記のURLに従いインポートします。

    https://zenn.dev/apple_nktn/articles/d6f7e0cac8d413

## 利用方法

### 事前準備

※`app/src/main/java/MainActivity.java`にサンプルコードが記載されています。

- パーミッションの確認と必要であればリクエストを行います。Androidのバージョンによって位置情報の権限が必要な場合とBluetooth系の権限が必要な場合があります。

    ```
    if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        mPermissionGranted = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN}, 0);
        } else {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH}, 0);
        }
    } else {
        mPermissionGranted = true;
    }
    ```


### ORPHE INSOLEの場合

- `OrpheInsoleCallback`オブジェクトを作成します。この中にORPHE INSOLEの各イベントに対しての動作を記述します。

    ```
    private final OrpheInsoleCallback mOrpheInsoleCallback = new OrpheInsoleCallback() {
        @Override
        public void gotInsoleValues(OrpheInsoleValue[] values) {
            Log.d(TAG, values[0].toString());
        }

        @Override
        public void gotDeviceInfo(DeviceInfoValue value) {
            Log.d(TAG, (value.batteryStatus.name());
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onScan(BluetoothDevice bluetoothDevice, OrpheScanedMeta meta) {
            if (mConnectionStatusTextViewLeft != null) {
                if (bluetoothDevice != null) {
                    final String deviceId = meta.deviceId;
                    Log.d(TAG, String.format("%s：機器が見つかりました", deviceId));
                } else {
                    Log.d(TAG, "機器が見つかりませんでした");
                }
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onConnect(BluetoothDevice bluetoothDevice) {
            if (mConnectionStatusTextViewLeft != null) {
                Log.d(TAG, String.format("%s：機器に接続されました", bluetoothDevice.getName()));
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onDisconnect(BluetoothDevice bluetoothDevice) {
            if (mConnectionStatusTextViewLeft != null) {
                Log.d(TAG, String.format("%s：機器の接続が解除されました", bluetoothDevice.getName()));
            }
        }
    };
    ```

- 作成した`OrpheInsoleCallback`と対応する取り付け位置（[OrpheSidePosition.leftPlanter]など）を指定して`OrpheInsole`オブジェクトを作成します。

    ```
    mOrpheInsole = new OrpheInsole(this, mOrpheInsoleCallbackLeft, OrpheSidePosition.leftPlantar);
    ```

- 作成した`OrpheInsole`オブジェクトの`startScan`を呼び出します。

    ```
    mOrpheInsole.startScan();
    ```

- ORPHE INSOLEのリセットボタンを押し光らせます。
    - 光った場合はアドバタイズしている状態になります。

- `OrpheInsoleCallback`の`onScan`に見つかったアドバタイズ中のORPHE INSOLEの`BluetoothDevice`オブジェクトが渡されます。

- `OrpheInsoleCallback`の`onScan`で渡された`BluetoothDevice`を`OrpheInsole`オブジェクトの`connect`に渡すことで接続されます。
    ```
    mOrpheInsole.connect(mBluetoothDevice);
    ```
  
    - `OrpheScanedMeta`の`deviceId`を参照することでデバイスIDを取得できます。
    - `OrpheScanedMeta`の`chargeStatus`を参照することで充電の状況（Wireless or Wired）。

- 接続後`OrpheInsoleCallback`の`onConnect`のコールバックが呼び出されます。

- センサー値の取得に関して

    - `OrpheInsoleSensorConfig`を指定すると、接続後の初期設定として受信方式とサンプリングレートを選択できます。

        ```
        OrpheInsoleSensorConfig config = new OrpheInsoleSensorConfig(
                OrpheSensorReceiveMode.realtime,
                OrpheInsoleSamplingRate.hz200
        );
        mOrpheInsole = new OrpheInsole(
                this,
                mOrpheInsoleCallback,
                OrpheSidePosition.leftPlantar,
                OrpheAccRange.range16,
                OrpheGyroRange.range2000,
                false,
                config
        );
        ```

        - `OrpheSensorReceiveMode.realtime`: デバイスが送信したセンサー値をそのままNotifyで受け取ります。
        - `OrpheSensorReceiveMode.request`: アプリから指定した範囲だけを手動でリクエストします。
        - `OrpheSensorReceiveMode.bestEffort`: SDKがPython版ところてんと同じcarry-over方式で常時リクエストし、欠損検出・再要求・重複排除・全体値へのシリアル順マージまで自動で行います。受信値のコールバックは欠損回収を待たず即時に呼ばれます。
        - `OrpheInsoleSamplingRate.hz100`: 100Hz出力。`realtime`ではクオータニオン付きの0x38を受信します。`request` / `bestEffort`ではFWの200Hz蓄積データ（0x36）をSDK内で姿勢計算してから100Hzへ間引きます。
        - `OrpheInsoleSamplingRate.hz200`: 200Hz出力。`realtime`ではクオータニオンを含まない0x37、`request` / `bestEffort`ではFWの蓄積・再要求形式である0x36を受信し、SDK内でクオータニオンを計算します。

        SDK利用側で欠損を可能な限り回収しながら100Hz出力を得る場合は以下のように指定します。FWから受け取る0x36は200Hz固定で、SDKは4点すべてを5ms間隔で姿勢計算した後、0ms / 10msの2点へ間引きます。アプリ側に姿勢計算用タイマーや再要求処理は不要です。

        ```
        OrpheInsoleSensorConfig config = new OrpheInsoleSensorConfig(
                OrpheSensorReceiveMode.bestEffort,
                OrpheInsoleSamplingRate.hz100
        );
        ```

        BestEffortの既定値は、200msごとに最大200シリアルを確認し、応答を最大5秒待ちます。Android版では応答を1件以上受信した後に250ms無通信となった時点で要求を終了し、未着値を次回へcarry-overするため、部分的なBLEロスのたびに5秒待ち続けません。FWから`noData`が返った値だけを回復不能として`OrpheInsoleCallback.sensorValueIsNotFound`へ通知します。carry-overが100シリアルを超えた場合、または最新値との差が1,500シリアルを超えた場合は、FWリングバッファの安全範囲へ再同期します。

        BestEffortでは、値を受信するたびに`OrpheInsoleValueUpdate`版のコールバックが呼ばれます。`getDeltaValues()`は今回受信した差分、`getAllValues()`はその時点までにSDKが取得・欠損回収できた全値です。欠損パケットが後から回収された場合は、その位置から後続のクオータニオンを時系列順に再計算します。全値はシリアル順にマージされ、実際に`getAllValues()`を呼んだ時だけ配列が生成されます。

        ```
        OrpheInsoleCallback callback = new OrpheInsoleCallback() {
            @Override
            public void gotInsoleValues(OrpheInsoleValueUpdate update) {
                OrpheInsoleValue[] deltaValues = update.getDeltaValues();
                OrpheInsoleValue[] allValues = update.getAllValues();
            }
        };
        ```

        従来の`gotInsoleValues(OrpheInsoleValue[] values)`を実装している場合も変更は不要です。BestEffortでは同メソッドへ即時差分が渡されます。現在の全値だけを後から参照する場合は`OrpheInsole#getBestEffortValues()`も利用できます。

        サンプルアプリではBestEffortを選ぶと200Hzへ固定され、サンプリングレート選択は無効になります。BestEffortではSDK計算値をQuaternionグラフへ表示します。`realtime`の200Hzは今回の計算対象外のため、Quaternionグラフを表示しません。

    - `OrpheSensorReceiveMode.request`で手動取得する場合は、接続後にセンサー値送信のリクエストを送ります。1範囲なら`requestInsoleValue(startSerialNumber, length)`、複数範囲なら`requestInsoleValue(OrpheValueRequest[])`を使用できます（最大30範囲）。

        ```
        mOrpheInsole.requestInsoleValue(1200, 100);
        ```

        - `requestLatestInsoleValue`を呼び出すことで最新のセンサー値を取得することが可能です。

            ```
            mOrpheInsole.requestLatestInsoleValue();
            // もしくは
            mOrpheInsole.requestLatestInsoleValue(100);
            ```

            - `length`のパラメーターを指定した場合は、**センサー値の最終取得時刻から予想されるシリアル番号からlength件**を取得します。
                - 取得件数が少なすぎると時間のズレ等でうまく取得できない可能性があるので100件程度までは数を増やしてください。
            - `length`のパラメータを指定しない場合は、**センサー値の最終取得時刻から予想されるシリアル番号から現在時刻までのセンサー値を取得**します。
        
        - `requestInsoleValue`を呼び出すことで自由にデバイス内に位置時保存されているセンサー値を取得することができます。
            - `OrpheValueRequest`に最初のシリアル番号とそこから取得する件数を指定してパラメータに渡してください。（最大30種類リクエストを送ることが可能）
        
        - 新しい手動リクエストはデバイス上の古いリクエストを置き換えます。常時取得には、要求を直列化する`bestEffort`を使用してください。
        
    - リクエストされたセンサー値は200Hz形式の0x36としてNotifyで送信され、SDKがクオータニオンを計算してから`OrpheInsoleCallback`の`gotInsoleValues`に渡します。BestEffortでは欠損箇所以降も待機させず、受信できた値から即時に通知し、欠損回収後にSDK内の後続値を再計算します。
        - `hz200`では各圧力センサー値を4つまとめて取得します。`hz100`ではSDKがその4点を2点へ間引きます。どちらもシリアル番号1増えるごとのインターバルは理論上20msです。
        - タイムスタンプはナノ秒なので秒に変換したい場合は1000000で割ります。

- 作成した`OrpheInsole`オブジェクトの`disconnect`を呼び出すことで切断します。

    ```
    mOrpheInsole.disconnect();
    ```

- 作成した`OrpheInsole`オブジェクトの`status`で現在の接続ステータスを把握することが可能です。

- 作成した`OrpheInsole`オブジェクトの`getDeviceInfo`を呼び出すことでバッテリー情報を含むORPHE INSOLEの情報を取得することができます。取得した値は`OrpheInsoleCallback`の`gotDeviceInfo`に渡されます。

    ```
    mOrpheInsole.getDeviceInfo();
    ```

- リアルタイムモードへの切り替えは`setSensorRequestMode`を呼び出すことで可能です。
    - リセットされるともとに戻るため接続時に１度だけ実行することを推奨します。
    - `OrpheInsoleSensorConfig`を指定した場合はSDKがNotify開始後に自動で切り替えます。
    - インソール（圧力込み）用の200Hzリアルタイムモードは`OrpheSensorRequestMode.realtimeForInsole`、100Hzリアルタイムモードは`OrpheSensorRequestMode.realtimeForInsoleWithQuaternion`と指定します。

    ```
    mOrpheInsole.setSensorRequestMode(OrpheSensorRequestMode.realtimeForInsole);
    ```

- 圧力の変換係数は`setPressureCalibration`メソッドで6点分を一括設定できます。各部位に`coefficient1`、`coefficient2`、`coefficient3`、`threshold`を設定できます。

    ```
    OrpheInsolePressureCalibration calibration = new OrpheInsolePressureCalibration(
            // toeInside
            new OrpheInsolePressureCoefficient(2.77942, 0.00235, 4.14411, 240.0),
            // midInside
            new OrpheInsolePressureCoefficient(2.70000, 0.00230, 4.10000, 235.0),
            // toeOutside
            new OrpheInsolePressureCoefficient(2.80000, 0.00240, 4.20000, 245.0),
            // center
            new OrpheInsolePressureCoefficient(2.60000, 0.00220, 4.00000, 230.0),
            // midOutside
            new OrpheInsolePressureCoefficient(2.90000, 0.00250, 4.30000, 250.0),
            // heel
            new OrpheInsolePressureCoefficient(3.00000, 0.00260, 4.40000, 255.0)
    );
    mOrpheInsole.setPressureCalibration(calibration);
    ```

    - `OrpheInsolePressureCoefficient`の4引数は順に`coefficient1`、`coefficient2`、`coefficient3`、`threshold`です。
    - 従来の2引数コンストラクタも利用でき、その場合は`coefficient2 = 0.00235`、`threshold = 240mV`が補完されます。
    - コンストラクタの部位順は`toeInside`、`midInside`、`toeOutside`、`center`、`midOutside`、`heel`です。
    - 既定値へ戻す場合は`setPressureCalibration(OrpheInsolePressureCalibration.DEFAULT)`を使用します。
    - 従来の`setCoefficient(sensorPosition, coefficient, value)`も引き続き利用でき、現在の一括設定のうち指定した1項目だけを更新します。
    - 計算式は`coefficient1 * exp(coefficient2 * milliVolt) + coefficient3`です。
    - `milliVolt <= threshold`の場合は`0N`として扱います。
    - 閾値以下、負の計算結果、または異常な入力値は`0N`として扱います。

#### サンプルアプリで補正値を設定する

- 左右のインソールへ接続すると、それぞれの`Pressure Calibration`ボタンが有効になります。
- 6点の圧力センサーごとに`coefficient1`、`coefficient2`、`coefficient3`、`threshold`を入力し、`Save`を押すと受信中の圧力値へ即時反映されます。
- 設定はデバイスID別に端末へ保存され、同じデバイスへ再接続したときに自動で適用されます。
- `Reset to defaults`を押すと、そのデバイスの保存値を削除し、全6点を既定値へ戻します。
- サンプルアプリは補正値を自動推定しません。測定などで決定した補正値を手動入力するための機能です。

### ORPHE COREの場合

- `OrpheCallback`オブジェクトを作成します。この中にORPHE COREの各イベントに対しての動作を記述します。

    ```
    private final OrpheCoreCallback mOrpheCoreCallback = new OrpheCoreCallback() {
        @Override
        public void gotSensorValues(OrpheSensorValue[] values) {
            Log.d(TAG, values[0].toString());
        }

        @Override
        public void gotDeviceInfo(DeviceInfoValue value) {
            Log.d(TAG, (value.batteryStatus.name());
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onScan(BluetoothDevice bluetoothDevice, OrpheScanedMeta meta) {
            if (mConnectionStatusTextViewLeft != null) {
                if (bluetoothDevice != null) {
                    final String deviceId = meta.deviceId;
                    Log.d(TAG, String.format("%s：機器が見つかりました", deviceId));
                } else {
                    Log.d(TAG, "機器が見つかりませんでした");
                }
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onConnect(BluetoothDevice bluetoothDevice) {
            if (mConnectionStatusTextViewLeft != null) {
                Log.d(TAG, String.format("%s：機器に接続されました", bluetoothDevice.getName()));
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onDisconnect(BluetoothDevice bluetoothDevice) {
            if (mConnectionStatusTextViewLeft != null) {
                Log.d(TAG, String.format("%s：機器の接続が解除されました", bluetoothDevice.getName()));
            }
        }
    };
    ```

- 作成した`OrpheCoreCallback`と対応する取り付け位置（[OrpheSidePosition.leftPlanter]など）を指定して`Orphe`オブジェクトを作成します。

    ```
    mOrphe = new Orphe(this, mOrpheCoreCallbackLeft, OrpheSidePosition.leftPlantar);
    ```

- 作成した`Orphe`オブジェクトの`startScan`を呼び出します。

    ```
    mOrphe.startScan();
    ```

- ORPHE COREを振り光らせます。
    - 光った場合はアドバタイズしている状態になります。

- `OrpheCoreCallback`の`onScan`に見つかったアドバタイズ中のORPHE COREの`BluetoothDevice`オブジェクトが渡されます。

- `OrpheCoreCallback`の`onScan`で渡された`BluetoothDevice`を`Orphe`オブジェクトの`connect`に渡すことで接続されます。
    ```
    mOrphe.connect(mBluetoothDevice);
    ```

    - `OrpheScanedMeta`の`deviceId`を参照することでデバイスIDを取得できます。

- 接続後`OrpheCoreCallback`の`onConnect`のコールバックが呼び出されます。

- センサー値の取得に関して

    - `OrpheCoreSensorConfig`でINSOLEと同じ3つの受信方式を指定できます。

        ```
        OrpheCoreSensorConfig config = new OrpheCoreSensorConfig(
                OrpheSensorReceiveMode.bestEffort
        );
        mOrphe = new Orphe(
                this,
                mOrpheCoreCallback,
                OrpheSidePosition.leftInstep,
                config
        );
        ```

        - `realtime`: リアルタイムNotify
        - `request`: アプリが指定した範囲だけを手動取得
        - `bestEffort`: SDKがPython版ところてんと同じcarry-over方式で継続取得と欠損回収を自動実行

    - `request`で手動取得する場合は、接続後にリクエストを送ります。1範囲なら`requestSensorValue(startSerialNumber, length)`、複数範囲なら`requestSensorValue(OrpheValueRequest[])`を使用できます（最大30範囲）。

        ```
        mOrphe.requestSensorValue(1200, 100);
        ```

        - `requestLatestSensorValue`を呼び出すことで最新のセンサー値を取得することが可能です。

            ```
            mOrphe.requestLatestSensorValue();
            // もしくは
            mOrphe.requestLatestSensorValue(100);
            ```

            - `length`のパラメーターを指定した場合は、**センサー値の最終取得時刻から予想されるシリアル番号からlength件**を取得します。
                - 取得件数が少なすぎると時間のズレ等でうまく取得できない可能性があるので100件程度までは数を増やしてください。
            - `length`のパラメータを指定しない場合は、**センサー値の最終取得時刻から予想されるシリアル番号から現在時刻までのセンサー値を取得**します。
        
        - `requestSensorValue`を呼び出すことで自由にデバイス内に位置時保存されているセンサー値を取得することができます。
            - `OrpheValueRequest`に最初のシリアル番号とそこから取得する件数を指定してパラメータに渡してください。（最大30種類リクエストを送ることが可能）
        
        - 新しい手動リクエストはデバイス上の古いリクエストを置き換えます。常時取得には、要求を直列化する`bestEffort`を使用してください。
        
    - リクエストされたセンサー値はNotifyで送信され、SDKがクオータニオン、Euler角、重力成分を計算してから`OrpheCoreCallback`の`gotSensorValues`に渡されます。
        - 200Hzで取得された各圧力センサー値を8つまとめて取得することができます。シリアル番号1増えるにつき8件のデータを取得することになるので**各シリアル番号ごとのインターバルは理論上40ms**になります。
        - タイムスタンプはナノ秒なので秒に変換したい場合は1000000で割ります。

    - `bestEffort`の既定値はCORE / INSOLE共通で、200msごと・最大200シリアル・応答待ち5秒・carry-over上限100シリアル・リングバッファ安全上限1,500シリアルです。Android版は応答を1件以上受信した後の無通信が250ms続くと早期に要求を終了します。BLEタイムアウトは次回要求へ持ち越し、デバイス内に対象シリアルが残っておらずFWから`noData`が返った場合だけ、`OrpheCoreCallback.sensorValueIsNotFound`（INSOLEでは`OrpheInsoleCallback.sensorValueIsNotFound`）が番号ごとに呼ばれます。

    - COREのBestEffortでもINSOLEと同様に、`OrpheSensorValueUpdate`から今回差分と再計算済み全値を取得できます。既存の配列版コールバックは変更不要です。

        ```java
        OrpheCoreCallback callback = new OrpheCoreCallback() {
            @Override
            public void gotSensorValues(OrpheSensorValueUpdate update) {
                OrpheSensorValue[] deltaValues = update.getDeltaValues();
                OrpheSensorValue[] allValues = update.getAllValues();
            }
        };

        OrpheSensorValue[] currentValues = mOrphe.getBestEffortValues();
        ```

    - SDK内の姿勢計算は`orphe_insole`と同じMadgwick 6軸フィルタ（初期姿勢`[w,x,y,z] = [1,0,0,0]`、`beta = 0.1`、`dt = 5ms`）を使用します。加速度・ジャイロのX/Y/ZはBLE復号値を並べ替え・符号反転せず使用し、ジャイロだけdpsからrad/sへ変換します。磁気センサーを使わないため、roll/pitchは重力で補正されますがyawには時間経過によるドリフトがあります。

- またセンサー値のNotifyが有効になり、`OrpheCoreCallback`の`gotSensorValues`に各Notifyごとで送信されたセンサー値が渡されます。（１度のNotifyで最大4つのセンサー値が渡されます）

    - Notifyは50Hzで送られており4つのセンサー値を送ることで最大200Hzのセンサー値を取得することができます。
    - タイムスタンプはナノ秒なので秒に変換したい場合は1000000で割ります。

- 作成した`Orphe`オブジェクトの`disconnect`を呼び出すことで切断します。

    ```
    mOrphe.disconnect();
    ```

- 作成した`Orphe`オブジェクトの`status`で現在の接続ステータスを把握することが可能です。

- 作成した`Orphe`オブジェクトの`getDeviceInfo`を呼び出すことでバッテリー情報を含むORPHE COREの情報を取得することができます。取得した値は`OrpheCoreCallback`の`gotDeviceInfo`に渡されます。

    ```
    mOrphe.getDeviceInfo();
    ```


## 変更要望や質問について

- ソースコードは下記のGithubで公開されています。

    - 基本的にソースを見ればすべてわかるようになっています。

        https://github.com/no-new-folk/orphe_core_sdk_for_android

- 質問や変更要望についてはGithubのissueに書いて頂けると幸いです。

    https://github.com/no-new-folk/orphe_core_sdk_for_android/issues

- オープンソースにしておりますのでORPHE COREに関係ないような機能の追加やインターフェースの変更についてはご自身でForkされて変更されても問題ございません。

    - 必要であればPullRequestを投げて頂けると適宜レビューの上マージさせていただきます。

        https://github.com/no-new-folk/orphe_core_sdk_for_android/pulls
