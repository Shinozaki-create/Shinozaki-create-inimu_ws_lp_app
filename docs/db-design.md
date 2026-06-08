# DB設計

DB初期化は `src/main/resources/schema.sql` と `data.sql` で行います。

## admin_users

管理者ユーザーです。メールアドレスはログインID、パスワードはBCryptハッシュです。

| カラム | 用途 |
| --- | --- |
| `email` | ログインID |
| `password_hash` | BCryptハッシュ |
| `role` | Spring Securityのロール |
| `enabled` | 有効状態 |

## workshop_schedules

開催日を管理します。`schedule_date` はユニークです。

## workshop_time_slots

開催日の時間枠です。`schedule_id` と `start_time` はユニークです。予約時は悲観ロックで取得し、同時予約による過剰予約を抑制します。

## reservations

予約情報です。予約時に `system_settings.price_per_person` から合計金額を計算します。

ステータス:

- `PENDING`
- `CONFIRMED`
- `CANCELLED`
- `COMPLETED`
- `NO_SHOW`

`CANCELLED` に変更した場合、該当時間枠の `reserved_count` を予約人数分戻します。二重キャンセルでは二重減算しません。

## inquiries

問い合わせ情報です。`POST /api/reservations` に `inquiryOnly=true` が指定された場合はこちらに保存します。

ステータス:

- `OPEN`
- `IN_PROGRESS`
- `CLOSED`

## system_settings

システム設定です。初期値として `price_per_person = 5500` を登録します。
