---
name: flutter-to-compose-migration
description: Flutter アプリ (`flutter_app/`) を Android 専用の Jetpack Compose 構成へ移行するための実装スキル。Flutter 画面の棚卸し、Compose 画面設計、状態管理移植、Repository 移植、テスト移植、段階リリース準備を行うときに使う。
---

# Flutter to Compose Migration Skill

## ゴール
- `flutter_app/` の既存仕様を Compose で再現する
- Android 向けに保守しやすい実装へ統一する

## 入力
- 既存 Flutter 実装（`flutter_app/`）
- 既存仕様メモ、または画面挙動が分かる情報

## 出力
- Compose 実装（画面・ViewModel・データ層）
- 移行進捗チェックリスト
- テスト（ユニット/UI）

## 手順
1. `flutter_app/` の画面・機能・遷移・状態を棚卸しする
2. 「まず同等再現」「後で改善」の境界を明文化する
3. Kotlin 側のパッケージ構成を先に固定する
4. データモデルと Repository を移植する
5. ViewModel と `UiState` を実装する
6. Compose 画面を1画面ずつ移植する
7. 画面ごとにテストと手動検証を実施する
8. 置換済み機能から Flutter 依存を段階削除する

## 設計ルール
- 状態は `ViewModel` に集約し、Composable は表示に専念させる
- 画面状態は単一 `UiState` で管理し、イベントで更新する
- 非同期処理は Coroutines + Flow を使う
- DI は Hilt を使用する（手動DIの新規追加はしない）
- ナビゲーションは Navigation Compose に統一する
- iOS配慮の抽象化は追加しない（Android専用）

## 品質ルール
- 仕様差分がある場合は必ず TODO ではなく課題として記録する
- 重大導線（起動、一覧、詳細、保存、設定）はUIテスト対象に含める
- クラッシュ要因（null、非同期キャンセル、再コンポーズ副作用）を先に潰す

## Definition of Done
- 主要画面が Compose 側で動作し、旧Flutter挙動と同等
- ViewModel / Repository の主要分岐にユニットテストがある
- 主要導線の UI テストが通る
- Flutter 側を更新しなくても開発継続できる
