# 2026-04-12 LiteRT-LM / Gemma 4 AIアドバイス機能

## 1. 変更ファイル一覧
- `app/build.gradle.kts`
- `app/src/androidTest/java/jp/hotdrop/simpledyphic/phase8/Phase8UserFlowUiTest.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/jp/hotdrop/simpledyphic/data/ai/LiteRtLmAdviceClient.kt`
- `app/src/main/java/jp/hotdrop/simpledyphic/data/local/AppSettingsLocalDataSource.kt`
- `app/src/main/java/jp/hotdrop/simpledyphic/data/local/RoomRecordLocalDataSource.kt`
- `app/src/main/java/jp/hotdrop/simpledyphic/data/local/db/AppDatabase.kt`
- `app/src/main/java/jp/hotdrop/simpledyphic/data/local/db/AppDatabaseMigrations.kt`
- `app/src/main/java/jp/hotdrop/simpledyphic/data/local/db/AppSettingsDao.kt`
- `app/src/main/java/jp/hotdrop/simpledyphic/data/local/db/AppSettingsEntity.kt`
- `app/src/main/java/jp/hotdrop/simpledyphic/data/local/db/RecordDao.kt`
- `app/src/main/java/jp/hotdrop/simpledyphic/data/repository/AppSettingsRepository.kt`
- `app/src/main/java/jp/hotdrop/simpledyphic/data/repository/GoalRepository.kt`
- `app/src/main/java/jp/hotdrop/simpledyphic/data/repository/HealthConnectRepository.kt`
- `app/src/main/java/jp/hotdrop/simpledyphic/data/repository/RecordRepository.kt`
- `app/src/main/java/jp/hotdrop/simpledyphic/di/DatabaseModule.kt`
- `app/src/main/java/jp/hotdrop/simpledyphic/domain/usecase/ExerciseAdviceInputBuilder.kt`
- `app/src/main/java/jp/hotdrop/simpledyphic/domain/usecase/GenerateExerciseAdviceUseCase.kt`
- `app/src/main/java/jp/hotdrop/simpledyphic/model/AdvicePeriod.kt`
- `app/src/main/java/jp/hotdrop/simpledyphic/model/AppSettings.kt`
- `app/src/main/java/jp/hotdrop/simpledyphic/model/ExerciseAdviceInput.kt`
- `app/src/main/java/jp/hotdrop/simpledyphic/model/ExerciseAdviceRequirement.kt`
- `app/src/main/java/jp/hotdrop/simpledyphic/model/ExerciseAdviceResult.kt`
- `app/src/main/java/jp/hotdrop/simpledyphic/model/ExerciseMetricKind.kt`
- `app/src/main/java/jp/hotdrop/simpledyphic/model/ExerciseMetricSummary.kt`
- `app/src/main/java/jp/hotdrop/simpledyphic/ui/MainNavigation.kt`
- `app/src/main/java/jp/hotdrop/simpledyphic/ui/ai/ExerciseAdviceScreen.kt`
- `app/src/main/java/jp/hotdrop/simpledyphic/ui/ai/ExerciseAdviceUiState.kt`
- `app/src/main/java/jp/hotdrop/simpledyphic/ui/ai/ExerciseAdviceViewModel.kt`
- `app/src/main/java/jp/hotdrop/simpledyphic/ui/settings/AiAdviceSettingsScreen.kt`
- `app/src/main/java/jp/hotdrop/simpledyphic/ui/settings/AiAdviceSettingsUiState.kt`
- `app/src/main/java/jp/hotdrop/simpledyphic/ui/settings/AiAdviceSettingsViewModel.kt`
- `app/src/main/java/jp/hotdrop/simpledyphic/ui/settings/SettingsScreen.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/test/java/jp/hotdrop/simpledyphic/data/local/db/AppDatabaseMigrationTest.kt`
- `app/src/test/java/jp/hotdrop/simpledyphic/data/local/db/AppSettingsDaoTest.kt`
- `app/src/test/java/jp/hotdrop/simpledyphic/domain/usecase/ExerciseAdviceInputBuilderTest.kt`
- `gradle/libs.versions.toml`

## 2. 実施内容
- BottomNavigation に `AI` タブを追加し、AI 画面と AI アドバイス設定画面を新設した。
- Room に `app_settings` テーブルと migration を追加し、生年月日・身長・体重・Gemma 4 モデル・指示文を保存できるようにした。
- LiteRT-LM の Android 依存を追加し、Gemma 4 の単発ストリーミング推論を行う `LiteRtLmAdviceClient` を追加した。
- Health Connect の複数指標権限確認 API、Record の日付範囲取得 API、AI 用の期間集計・プロンプト組み立て UseCase を追加した。
- 新規 DAO / UseCase / Compose UI 導線向けの unit test と androidTest compile 対応を追加した。

## 3. 実行したテスト/確認結果
- `./gradlew :app:compileDebugKotlin`
  - 成功
- `./gradlew :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin`
  - 成功

## 4. 残課題・次アクション
- 実機またはエミュレータで `.litertlm` モデル選択、初回モデル起動時間、GPU/CPU フォールバック挙動の確認が未実施。
- LiteRT-LM の `latest.release` 運用は公式推奨に合わせたが、将来的には固定 version への pin を再検討したい。
- 作業ツリーには `.idea/appInsightsSettings.xml`、`gradle.properties`、`gradle/wrapper/gradle-wrapper.properties` など既存の未関連変更が残っているため、コミット時は切り分け確認が必要。

# 2026-04-12 README への LiteRT-LM モデル利用手順追記

## 1. 変更ファイル一覧
- `README.md`

## 2. 実施内容
- README に AIアドバイス機能で使う `.litertlm` モデルの入手元、設定画面での取り込み手順、非自動ダウンロードであること、課金・ライセンス・安全性に関する注意点を追記した。

## 3. 実行したテスト/確認結果
- 手動確認
  - `README.md` の追記内容が、現行実装のモデル選択フローと一致することをコード確認で照合した。

## 4. 残課題・次アクション
- README には一般的な取得元のみ記載しており、採用モデル名や推奨サイズを固定する場合は別途追記が必要。
- 将来的にアプリ内ダウンロード機能を追加する場合は、README の説明も実装に合わせて更新が必要。
