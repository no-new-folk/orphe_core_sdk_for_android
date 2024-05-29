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

- `OrpheCallback`オブジェクトを作成します。この中にORPHE COREの各イベントに対しての動作を記述します。

    ```
    private final OrpheCallback mOrpheCallback = new OrpheCallback() {
        @Override
        public void gotSensorValues(OrpheSensorValue[] values) {
            Log.d(TAG, values[0].toString());
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

- 作成した`OrpheCallback`と対応する取り付け位置（[OrpheSidePosition.leftPlanter]など）を指定して`Orphe`オブジェクトを作成します。

    ```
    mOrphe = new Orphe(this, mOrpheCallbackLeft, OrpheSidePosition.leftPlantar);
    ```

- 作成した`Orphe`オブジェクトの`startScan`を呼び出します。

    ```
    mOrphe.startScan();
    ```

- ORPHE COREを振り光らせます。
    - 光った場合はアドバタイズしている状態になります。

- `OrpheCallback`の`onScan`に見つかったアドバタイズ中のORPHE COREの`BluetoothDevice`オブジェクトが渡されます。

- `OrpheCallback`の`onScan`で渡された`BluetoothDevice`を`Orphe`オブジェクトの`connect`に渡すことで接続されます。
    ```
    mOrphe.connect(mBluetoothDevice);
    ```

- 接続後`OrpheCallback`の`onConnect`のコールバックが呼び出されます。

- またセンサー値のNotifyが有効になり、`OrpheCallback`の`gotSensorValues`に各Notifyごとで送信されたセンサー値が渡されます。（１度のNotifyで最大4つのセンサー値が渡されます）

    - Notifyは50Hzで送られており4つのセンサー値を送ることで最大200Hzのセンサー値を取得することができます。
    - タイムスタンプはナノ秒なので秒に変換したい場合は1000000で割ります。

- 作成した`Orphe`オブジェクトの`disconnect`を呼び出すことで切断します。

    ```
    mOrphe.disconnect();
    ```

- 作成した`Orphe`オブジェクトの`status`で現在の接続ステータスを把握することが可能です。


## 変更要望や質問について

- ソースコードは下記のGithubで公開されています。

    - 基本的にソースを見ればすべてわかるようになっています。

        https://github.com/no-new-folk/orphe_core_sdk_for_android

- 質問や変更要望についてはGithubのissueに書いて頂けると幸いです。

    https://github.com/no-new-folk/orphe_core_sdk_for_android/issues

- オープンソースにしておりますのでORPHE COREに関係ないような機能の追加やインターフェースの変更についてはご自身でForkされて変更されても問題ございません。

    - 必要であればPullRequestを投げて頂けると適宜レビューの上マージさせていただきます。

        https://github.com/no-new-folk/orphe_core_sdk_for_android/pulls