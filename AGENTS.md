# AGENTS

このリポジトリはワークショップ予約サイトのバックエンドです。

- Java 21 / Spring Boot 3 系で実装します。
- アーキテクチャは Controller -> Service -> Repository を基本にします。
- 公開APIは `/api/**`、管理画面は `/admin/**` に集約します。
- DB初期化は `src/main/resources/schema.sql` と `data.sql` を使います。
- フロントエンド連携仕様は `docs/frontend-integration.md` に追記します。
