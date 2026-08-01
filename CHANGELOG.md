# Change Log

## Unreleased

### Changes

---

- **FEAT**: INSOLEのFIFO受信停止時に**回収フェーズ（drain / catch-up）**を追加。`setSensorConfig`でfifoから他モードへ切り替える際、停止時点でFWが生成済みのシリアルを固定ターゲットとして、未要求分（catch-up）と要求済み未受信分（carry-over）を回収してから新しい設定を適用します。従来は`stop()`が再要求キューを即座に破棄し、未要求のバックログも回収しなかったため、**欠損通知ゼロのまま収録末尾が数秒欠ける**ことがありました（2台同時収録の実測で、遅れた側が要求スパンの86〜89%しか記録されない例を確認。JS SDK [ORPHE-INSOLE.js#62](https://github.com/Orphe-OSS/ORPHE-INSOLE.js/pull/62) と同じ問題）。回収の上限時間は`OrpheFifoConfig.drainTimeoutMillis`（既定3000ms、`0`で従来どおり即時切替）。取りこぼしが無い正常系では1往復で完了します。回収できなかったシリアルは`sensorValueIsNotFound`で通知されます
- **FIX**: FIFO受信の停止時（切断・`close`を含む）、要求済みで未受信のシリアルを`sensorValueIsNotFound`で通知してから破棄するようにしました。従来は無通知で破棄され、アプリから欠損に気づけませんでした（CORE `Orphe`のFIFO経路も同様）
- **BREAKING**: INSOLEのパケット内フレーム時刻を、名目200Hz（5ms/frame）ではなく実測ODR **208Hz**（LSM6DSOXの標準ODR、約4.808ms/frame）で合成するようにしました。`OrpheInsoleValue`の`startTime`のパケット内間隔が約4%短くなります（200Hz経路: 5ms→約4.808ms、100Hz経路: 10ms→約9.615ms）。従来はパケット内だけが約4%引き伸ばされ、パケット境界で連続サンプルの時刻差が0や負になりえたため、サンプル毎のdtで角速度等を積分する処理が系統的にずれていました（JS SDK v1.3.1 の`IMU_ODR_HZ`と同じ根拠・同じ値）

## 2026-07-28 (2)

### Changes

---

- **FIX**: アドバタイズ名が`INS0…`のINSOLEがスキャンで見つからない不具合を修正。従来はメーカーデータ（Company ID 0）が取得できない時点で判定を打ち切っていたため、アドバタイズ名による判定に到達していませんでした
- **FEAT**: ORPHE INSOLEの判定に**アドバタイズ名の`INS`プレフィックス**を追加（`DeviceNameDefine.ORPHE_INSOLE`）。判定順をアドバタイズ名優先に変更し、メーカーデータを持たない機体も検出できるようにしました
- **FIX**: デバイス名の取得元を`BluetoothDevice.getName()`（キャッシュ由来。未ペアリング機体でnullになりやすい）から`ScanRecord.getDeviceName()`（アドバタイズ由来）優先に変更。CORE側（`Orphe`）も同様
- **FIX**: メーカーデータのシグネチャ判定に必要な長さを15バイトから7バイトに緩和。充電ステータスは15バイト以上の場合のみ読み取ります
- **FIX**: メーカーデータのCompany IDが0以外の機体も判定対象に含めるようにしました（Company ID 0を優先し、無い場合は先頭のエントリを使用）
- **FIX**: ORPHE CORE（`CR-`）の候補に対して、レイアウトの異なるメーカーデータの左右バイトでフィルタしていた問題を修正。左右フィルタはメーカーデータ経路のみに限定したため、**これまで弾かれていたCORE機器が候補に出るようになります**
- **FEAT**: アドバタイズから左右が判別できない機体を「左右不明候補」として左右両方に通知するようにしました。`OrpheScanedMeta`に`side` / `sideIsUnknown()`を追加。`OrpheInsoleScanConfig`と`OrpheInsole.setScanConfig`で無効化できます
- **FEAT**: `OrpheInsoleCallback`に`onScanFailed(int errorCode)`と`onSideMismatch(BluetoothDevice, OrpheSide, OrpheSide)`を追加。従来はスキャン失敗（権限不足・スキャン頻度制限など）が完全に無言だったため、原因の切り分けができませんでした
- **FIX**: スキャンのタイムアウトタイマーを`removeCallbacks`していなかったため、再スキャン時に前回のタイマーが新しいスキャンを停止して誤タイムアウトを通知していた問題を修正
- **FIX**: `OrpheInsole.connect`のログが引数の`device`ではなくフィールドの`mBluetoothDevice`を参照していたため、切断直後に候補リストから再接続するとNPEになる問題を修正
- **CHORE**: スキャン設定を`SCAN_MODE_LOW_LATENCY` / `MATCH_MODE_AGGRESSIVE`に変更。既定の`SCAN_MODE_LOW_POWER`は約5.12秒周期のうち0.512秒しか受信しないため、アドバタイズを取り逃していました
- **CHORE**: スキャン中の再スキャンを行わないようにしました。Androidのスキャン頻度制限（アプリあたり30秒に5回）を無駄に消費し、無言で0件になるのを防ぎます
- **CHORE**: サンプルアプリの`AndroidManifest.xml`で`BLUETOOTH_SCAN`に`android:usesPermissionFlags="neverForLocation"`を付与し、旧権限に`android:maxSdkVersion="30"`を指定。Android 11以下向けに位置情報サービスの有効判定も追加

## 2026-07-28

### Changes

---

- **CHORE**: SDK バージョンを 0.6.2 に更新し、`archives/orphecoresdk-release-0.6.2.aar` とサンプルAPK `orphe-core-sdk-sample-0.6.2.apk` を追加
- **BREAKING**: INSOLEのジャイロ物理値換算を、LSM6DSOXのデータシート感度（±2000dpsで70 mdps/LSB）に基づく`生値 × 感度[dps/LSB]`へ修正。従来の`生値 ÷ 32768 × レンジ`は約12.8%過小だったため、`OrpheInsoleValue`の`gyroX` / `gyroY` / `gyroZ`は約14.7%大きい値になります。旧バージョン（〜0.6.1）で記録した`gyro`列は`× 1.14688`（全レンジ共通）で補正できます
- **BREAKING**: 上記に伴い、INSOLEの`request` / `fifo`（200Hz）でSDKがMadgwick 6軸フィルタにより算出するクオータニオンの値も変化します。`realtime` 100Hzのデバイス算出クオータニオンは影響を受けません
- **NOTE**: 加速度・圧力・`realtime` 100Hzのクオータニオンは影響ありません。CORE（`OrpheSensorValue`）のジャイロ換算はIMUの感度をFWチームに確認中のため今回は据え置きです

## 2026-07-17

### Changes

---

- **CHORE**: SDK バージョンを 0.6.1 に更新し、`archives/orphecoresdk-release-0.6.1.aar` とサンプルAPK `orphe-core-sdk-sample-0.6.1.apk` を追加
- **BREAKING**: CORE / INSOLEのEuler角計算を一時停止し、`OrpheSensorValue` / `OrpheInsoleValue`から`eulerYaw`・`eulerPitch`・`eulerRoll`公開フィールドを削除
- **FIX**: サンプルアプリのCSV出力で、クオータニオン4列をRealtime 100Hzの場合だけ出力し、Request / FIFOおよびRealtime 200Hzではヘッダーごと除外。Euler角3列は全モードから削除

## 2026-07-14

### Changes

---

- **FEAT**: CORE / INSOLE 共通の受信方式として `realtime` / `request` / `fifo` を追加
- **BREAKING**: 受信方式名をFIFOへ完全変更し、公開APIを`OrpheSensorReceiveMode.fifo`、`OrpheFifoConfig`、`getFifoValues()`へ統一（旧名称の互換APIは提供しない）
- **FEAT**: `request`を手動要求専用とし、開始シリアル番号と件数を指定する公開APIをCORE / INSOLE双方に追加
- **FEAT**: `fifo`をCORE / INSOLE共通でPython版ところてんと同じcarry-over方式へ変更。BLEタイムアウトは固定回数で打ち切らず次回要求へ持ち越し、FWの`noData`・carry-over 100件超過・リングバッファ1,500件超過で再同期する仕様に統一
- **DEPRECATED**: `OrpheFifoConfig.maxRetryCount`と4引数コンストラクタを非推奨化（ソース互換のため維持し、新方式では固定リトライ回数として使用しない）
- **FIX**: INSOLEの`fifo`で欠損回収待ちがリアルタイム表示を停止しないよう、受信差分を即時通知する経路と全体値のシリアル順マージを分離
- **FIX**: Android版`fifo`は応答を1件以上受信した後、250ms無通信なら5秒の総タイムアウトを待たず未着シリアルをcarry-overし、左右同時取得時のリングバッファ超過を抑止
- **FIX**: INSOLEの`request` / `fifo`はFW蓄積形式の0x36（200Hz）だけを受理し、`hz100`指定時はSDKで0ms / 10msの2点へ間引くよう修正
- **FEAT**: `OrpheInsoleValueUpdate`を追加し、更新コールバックから今回の差分と、その更新時点までの欠損回収済み全値を取得可能に変更
- **FEAT**: CORE / INSOLEの`request` / `fifo`で、`orphe_insole`準拠のMadgwick 6軸フィルタにより加速度・ジャイロからクオータニオンを算出。欠損回収時は回収位置以降を時系列順に再計算
- **FEAT**: COREにも`OrpheSensorValueUpdate`と`getFifoValues()`を追加し、既存配列コールバックとの互換性を維持しながら差分と再計算済み全値を取得可能に変更
- **FEAT**: サンプルアプリの選択肢を通常利用向けの `realtime` / `fifo` に整理（SDKの手動 `request` APIは継続提供）
- **FIX**: サンプルアプリはFIFO選択時に200Hzへ固定し、Quaternionグラフを非表示。Quaternionグラフはrealtime 100Hz選択時だけ表示
- **FEAT**: サンプルアプリにINSOLEの6点別圧力補正設定UIを追加し、デバイスID別の保存・接続時自動適用・既定値リセットに対応
- **FEAT**: INSOLEの6点別圧力補正で`coefficient2`と`threshold`も設定可能にし、サンプルアプリの入力・保存・再接続時適用を4値すべてに拡張（従来の2引数APIと保存データは互換維持）

## 2026-07-13

### Changes

---

- **BREAKING**: INSOLEの圧力補正仕様をdemo010に統一し、センサー別に設定可能な係数をcoefficient1とcoefficient3のみに変更
- **FIX**: coefficient2を0.00235、閾値を240mVの固定値に変更し、負の計算結果と異常値を0Nに補正
- **FEAT**: `OrpheInsolePressureCalibration`と`OrpheInsolePressureCoefficient`を追加し、6点それぞれの補正値を`setPressureCalibration`で一括指定可能に変更（従来の`setCoefficient`も継続利用可能）
- **BREAKING**: `OrpheInsoleValue` / `OrpheSensorValue` に `receivedAt`（SDK受信時刻・epochミリ秒）を追加。両クラスのコンストラクタと `fromBytes` シグネチャに `long receivedAt` 引数が追加され、後方互換なし
- **FEAT**: サンプルアプリのインソールCSV出力に `receivedAt` 列を追加

## 2025-07-18

### Changes

---

- **FEAT**: OrpheInsoleCoefficientにthresholdを追加

## 2025-07-01

### Changes

---

- **FEAT**: setCoefficientメソッドで変換係数を設定可能

## 2025-06-30

### Changes

---

- **FIX**: インソールのAD-N変換の計算式の調整

## 2025-06-27

### Changes

---

- **FEAT**: 充電の状態をスキャン時に取得可能にした
- **FIX**: インソールのAD-N変換の計算式の調整

## 2025-03-２５

### Changes

---

- **FIX**: インソールIDを16進数でパースするようにした。

## 2025-03-13

### Changes

---

- **FIX**: スキャンがタイムアウトした場合、コールバックの前に強制的にステータスをnoneにするようにした。

## 2025-03-03

### Changes

---

- **FEAT**: スキャンデータに`OrpheScanedMeta`に変更し、デバイスIDを取得可能にした。

## 2025-01-10

### Changes

---

- **FIX**: 圧力の値の取得方法を修正

## 2024-12-20

### Changes

---

- **FEAT**: リアルタイム送信切り替え機能を追加

## 2024-12-16

### Changes

---

- **FIX**: データのリクエスト時の不具合を修正

## 2024-11-28

### Changes

---

- **FEAT**: ログの表示機能を追加
- **FIX**: キャラクタリスティックの書き込み方式をWRITE_TYPE_NO_RESPONSEに変更

## 2024-11-19

### Changes

---

- **FIX**: 圧力の位置を変更

## 2024-11-14

### Changes

---

- **FIX**: N計算の不具合修正

## 2024-11-13

### Changes

---

- **FIX**: GetCurrentSerialNumberにてデータも取得できてしまう不具合を修正
- **FIX**: 取得できない場合のコールバックを追加

## 2024-11-12

### Changes

---

- **FIX**: ニュートンに変換するための補正を追加
- **FIX**: インソールの左右情報を判別して接続
- **FIX**: インソールの値の名前を修正

## 2024-10-25

### Changes

---

- **FEAT**: リクエストによるセンサーデータの送信機能を追加
- **FIX**: 切断時connectingのステータスになる不具合を修正

## 2024-07-01

### Changes

---

 - **FIX**: 切断時にエラーが出る不具合を修正
 - **FEAT**: OrpheInsoleのクラスを利用可能にしインソールと接続可能な実装を追加
