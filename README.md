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

- `OrpheInsoleCallback`オブジェクトを作成します。この中にORPHE COREの各イベントに対しての動作を記述します。

    ```
    private final OrpheInsoleCallback mOrpheInsoleCallback = new OrpheInsoleCallback() {
        @Override
        public void gotInsoleValue(OrpheInsoleValue value) {
            Log.d(TAG, value.toString());
        }

        @Override
        public void gotDeviceInfo(DeviceInfoValue value) {
            Log.d(TAG, (value.batteryStatus.name());
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onScan(BluetoothDevice bluetoothDevice) {
            if (mConnectionStatusTextViewLeft != null) {
                if (bluetoothDevice != null) {
                    Log.d(TAG, String.format("%s：機器が見つかりました", bluetoothDevice.getName()));
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

- 接続後`OrpheInsoleCallback`の`onConnect`のコールバックが呼び出されます。

- またセンサー値のNotifyが有効になり、`OrpheInsoleCallback`の`gotInsoleValue`に各Notifyごとで送信されたセンサー値が渡されます。

    - Notifyは50Hzで送られており各圧力センサー値を取得することができます。
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
        public void onScan(BluetoothDevice bluetoothDevice) {
            if (mConnectionStatusTextViewLeft != null) {
                if (bluetoothDevice != null) {
                    Log.d(TAG, String.format("%s：機器が見つかりました", bluetoothDevice.getName()));
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

- 接続後`OrpheCoreCallback`の`onConnect`のコールバックが呼び出されます。

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