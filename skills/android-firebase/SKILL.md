---
name: android-firebase
description: このアプリの Firebase Auth / Firestore 連携実装スキル。Credential Manager を使う Google サインイン、バックアップ/リストア同期、RemoteDataSource 設計、エラーハンドリングを実装・改修するときに使う。
---

# Android Firebase Skill

## 認証実装ルール
- Google サインインは `CredentialManager` + Firebase Auth の既存経路に統一する。
- `FirebaseAuthRemoteDataSource` で SDK 呼び出しを完結させ、Repository で結果型へ変換する。
- サインアウト時は Firebase 側のサインアウトと資格情報クリアをセットで実行する。
- 認証失敗は `AppResult.Failure` / `AppCompletable.Failure` で返し、UI は状態で表示制御する。

## Firestore 実装ルール
- 保存先コレクション構造（`dyphic/{userId}/records/{recordId}`）を維持する。
- バックアップ/リストアのオーケストレーションは `RecordRepository` に置く。
- ドキュメント変換は RemoteDataSource に閉じ、型変換失敗時は安全側で扱う。
- ログインしていない場合の backup/restore は既存方針どおり no-op 完了を返す。

## データ整合性ルール
- ローカル Room を正とし、Firestore は同期先として扱う。
- restore 実行時は既存方針に沿ってローカル全置換を行う。
- 数値や nullable 項目の変換は null 安全に行い、変換不能データでクラッシュしない。

## セキュリティと運用
- API キーや client id をコード直書きしない。リソース/設定経由で参照する。
- ユーザー向けメッセージと内部ログを分離する。
- Firebase 例外は型を保持したままログへ残し、UI には一般化したメッセージを渡す。

## テストルール
- Repository 層で未ログイン・成功・失敗の主要分岐をユニットテストする。
- データ変換ロジック（Map <-> Record）の破損ケースをテストする。

## 完了チェック
- 認証・同期の責務分離が維持される。
- 未ログイン分岐が壊れていない。
- データ変換失敗でクラッシュしない。
- 主要分岐のテストが追加または更新される。
