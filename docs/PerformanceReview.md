# Compose Performance/State Review

## 方針
- バックアップ/リストアは年1回運用想定のため、恒常的な性能最適化の優先度は下げる。
- 優先対象は画面実装: `SettingsScreen.kt` / `CalendarScreen.kt` / `RecordEditScreen.kt` とそのViewModel。
- 「Firestore backupで削除フィールドが反映されない」は本ドキュメントの対象外とする。

## 対応済み（最新）

### 1. [Done] Restoreのトランザクション化
- Files:
  - `app/src/main/java/jp/hotdrop/simpledyphic/data/local/db/RecordDao.kt:26`
  - `app/src/main/java/jp/hotdrop/simpledyphic/data/local/RoomRecordLocalDataSource.kt:25`
- 内容:
  - `RecordDao.replaceAll()` を `@Transaction` で実装。
  - `RoomRecordLocalDataSource.replaceAll()` はDAOのトランザクション関数を利用。

### 2. [Done] 全ViewModelをBaseViewModelへ統一
- Files:
  - `app/src/main/java/jp/hotdrop/simpledyphic/ui/calendar/CalendarViewModel.kt:21`
  - `app/src/main/java/jp/hotdrop/simpledyphic/ui/record/RecordEditViewModel.kt:32`
  - `app/src/main/java/jp/hotdrop/simpledyphic/ui/settings/SettingsViewModel.kt:26`
- 内容:
  - `BaseViewModel.launch` / `dispatcherIO` に統一し、適切なスコープ運用へ寄せた。

### 3. [Done] SettingsのUIスコープ依存を排除
- Files:
  - `app/src/main/java/jp/hotdrop/simpledyphic/ui/settings/SettingsScreen.kt:47`
  - `app/src/main/java/jp/hotdrop/simpledyphic/ui/settings/SettingsViewModel.kt:57`
- 内容:
  - `rememberCoroutineScope` を削除し、操作イベントはViewModel内で実行。
  - 回転時キャンセル耐性を改善。

### 4. [Done] Settingsの多重実行抑止
- Files:
  - `app/src/main/java/jp/hotdrop/simpledyphic/ui/settings/SettingsScreen.kt:209`
  - `app/src/main/java/jp/hotdrop/simpledyphic/ui/settings/SettingsViewModel.kt:58`
- 内容:
  - `isLoading` 中のUI操作を無効化。
  - ViewModel側でも `if (isLoading) return` で再入防止。

### 5. [Done] Repository返却をAppResult/AppCompletableへ統一
- Files:
  - `app/src/main/java/jp/hotdrop/simpledyphic/model/AppResult.kt:25`
  - `app/src/main/java/jp/hotdrop/simpledyphic/data/repository/AccountRepository.kt:16`
  - `app/src/main/java/jp/hotdrop/simpledyphic/data/repository/RecordRepository.kt:20`
  - `app/src/main/java/jp/hotdrop/simpledyphic/data/repository/HealthConnectRepository.kt:35`
- 内容:
  - `runCatching` 中心の分岐を縮小し、戻り値で成功/失敗を明示。
  - `CancellationException` は再throwして正しくキャンセル伝播。

### 6. [Done] CalendarをDB更新の継続購読へ変更
- Files:
  - `app/src/main/java/jp/hotdrop/simpledyphic/data/local/db/RecordDao.kt:17`
  - `app/src/main/java/jp/hotdrop/simpledyphic/data/local/RoomRecordLocalDataSource.kt:21`
  - `app/src/main/java/jp/hotdrop/simpledyphic/data/repository/RecordRepository.kt:37`
  - `app/src/main/java/jp/hotdrop/simpledyphic/ui/calendar/CalendarViewModel.kt:54`
- 内容:
  - Roomの `Flow` を使ってレコード一覧を継続購読。
  - `CalendarViewModel` は初回/再試行時に購読開始し、更新を自動で `UiState` に反映。

### 7. [Done] RecordEditのイベント配送を取りこぼさない構成へ変更
- Files:
  - `app/src/main/java/jp/hotdrop/simpledyphic/ui/record/RecordEditViewModel.kt:45`
  - `app/src/main/java/jp/hotdrop/simpledyphic/ui/record/RecordEditScreen.kt:84`
- 内容:
  - one-shot effectを `SharedFlow(replay=0)` から `Channel(BUFFERED)` + `receiveAsFlow()` に変更。
  - `onScreenEntered()` と `effects.collect` を別 `LaunchedEffect` に分離。

## 残課題（性能観点）

### 1. [P3] RecordEditで入力ごとに差分計算コストが高い
- File: `app/src/main/java/jp/hotdrop/simpledyphic/ui/record/RecordEditViewModel.kt:332`
- 問題:
  - すべての入力変更で `hasChangesFromState()` が全項目比較＋数値パースを実行している。
  - 入力頻度の高い画面で不要なCPU負荷になりやすい。
- 推奨:
  - 変更フラグを入力種別ごとに増分更新するか、重い比較処理を必要時（保存前など）へ遅延する。

### 2. [P3] Calendarの日セルで毎回日付判定とマーカー判定を繰り返している
- File: `app/src/main/java/jp/hotdrop/simpledyphic/ui/calendar/CalendarScreen.kt:286`
- 問題:
  - `DayCell` 毎に `LocalDate.now()` 呼び出しとマーカー条件評価を実施。
  - 月切替や再描画時にセル数分の計算が発生する。
- 推奨:
  - `today` を上位で一度だけ計算して渡す。
  - マーカー有無を `recordsByDate` 構築時に前計算し、セル側の判定を軽量化する。

## 検証メモ
- 実行コマンド:
  - `./gradlew :app:compileDebugKotlin`
  - `./gradlew :app:testDebugUnitTest`
- 結果:
  - いずれも `BUILD SUCCESSFUL`
- 備考:
  - `androidTest`（回転を含む実機/エミュレータ検証）は未実施
