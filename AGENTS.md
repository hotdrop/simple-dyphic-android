# AGENTS.md

このプロジェクトは **Flutter から Jetpack Compose への段階的移行** を目的とする。
以降、実装対象は Android のみとし、Dart/Flutter の新規実装は行わない。

## 目的
- `flutter_app/` の既存機能を Android (Kotlin + Jetpack Compose) に移植する
- 既存ユーザー体験を維持しつつ、保守性を Android 標準スタックへ寄せる
- 将来的な業務利用（Compose中心）に合わせて技術統一する

## 基本方針
- UI: Jetpack Compose (Material 3)
- 言語: Kotlin
- 非同期: Kotlin Coroutines + Flow
- 状態管理: `ViewModel` + `UiState`
- DI: Hilt を使用する（必須）
- 永続化: 既存要件に合わせて Room / DataStore を選定
- ナビゲーション: Navigation Compose

## 移行ルール
1. **機能同等性優先**
   - `flutter_app/` の挙動を先に再現し、仕様変更は別PR/別タスクに分離する。
2. **画面単位で移行**
   - 「画面 + 関連ロジック + テスト」を最小単位にして段階移行する。
3. **設計の分離**
   - UI (Compose), 状態管理 (ViewModel), ドメインロジック, データアクセスを分離する。
4. **Flutter依存の概念を直訳しない**
   - `Widget`/`BuildContext` 前提の実装を避け、Compose流に再設計する。
5. **再利用可能なUI部品を先に抽出**
   - 色・タイポ・間隔・共通コンポーネントは早期に共通化する。
6. **Android専用最適化を許容**
   - iOS互換目的の抽象化は追加しない。

## 実装規約
- 1ファイル肥大化を避け、責務ごとに分割する
- `@Composable` は副作用を最小化し、状態は上位から渡す
- 画面Composableを追加/更新する場合は、必ず`@Preview`を実装する
- `@Preview`はViewModel非依存にする（ViewModelのモックは禁止）。`Route`と`Screen`を分離し、Previewは`Screen`を直接描画する
- 条件分岐で表示されるUI（`if`/`when`/`?.let`で出るエラー、ダイアログ、ローディング、メッセージ等）は、すべて個別Previewで確認できるようにする
- Previewで表示できないUIは実装しない。画面レビュー時にUI要素が隠れない構成を必須とする
- 画面状態は `sealed interface/class` か `data class` で明示する
- 文字列/色/寸法はリソースまたはテーマで管理する
- 例外処理は握りつぶさず、ユーザー向けメッセージとログを分離する
- DIコンテナは Hilt に統一し、手動DIの新規追加は行わない

## テスト規約
- ViewModel: 状態遷移と入力検証をユニットテスト
- Repository/UseCase: 主要分岐をユニットテスト
- UI: 重要導線のみ Compose UI Test を追加
- 移行完了判定は「主要機能の同等性 + テスト通過 + 手動確認」で行う

## 推奨移行手順
1. `flutter_app/` の画面・機能・データ構造を棚卸しする
2. Compose側の画面一覧と遷移図を作成する
3. データ層（モデル/Repository）を先に移植する
4. 画面を優先度順に移植する（利用頻度の高い順）
5. 画面ごとにテスト追加と手動検証を実施する
6. 置き換え完了後に不要なFlutter資産を段階削除する

## 完了条件
- `flutter_app/` で提供していた主要機能が Compose 側で再現されている
- 主要導線でクラッシュ/重大な表示崩れがない
- CI（またはローカル）でテストが安定して通る
- Flutter実装に新規依存しない運用へ移行できている
