# Frontend Integration

このリポジトリでは、フロントエンドは Spring Boot の MVC 構成で配信します。
ホーム画面は `src/main/resources/templates/index.html` を Thymeleaf テンプレートとして返し、
静的ファイルは `src/main/resources/static/assets/` 配下に置きます。

## View / Static Assets

- `src/main/resources/templates/index.html`
- `src/main/resources/static/assets/css/style.css`
- `src/main/resources/static/assets/js/script.js`
- `src/main/resources/static/assets/img/**`
- `src/main/resources/static/assets/icons/**`

## Routing

- `GET /` - public homepage
- `GET /index` - public homepage
- `GET /todo` - redirect to `/admin`
- `/api/**` - public API endpoints used by the homepage

## Public API

- `GET /api/schedules`
- `GET /api/schedules/{date}/slots`
- `POST /api/reservations`

## Frontend Flow

1. ページ表示時に `GET /api/schedules` を呼び出し、開催日カレンダーを描画します。
2. 日付を選ぶと `GET /api/schedules/{date}/slots` を呼び出し、時間帯と残席数を更新します。
3. 予約内容を確認した後に `POST /api/reservations` を送信します。
4. `inquiryOnly=true` の場合は問い合わせ扱いとして送信します。

## Request Body

フロントエンドは snake_case と camelCase の両方を扱えるようにしています。

```json
{
  "inquiryOnly": false,
  "reservationDate": "2026-06-06",
  "reservationTime": "11:00",
  "reservationCount": 2,
  "customerFamilyName": "山田",
  "customerGivenName": "花子",
  "customerFamilyKana": "ヤマダ",
  "customerGivenKana": "ハナコ",
  "customerEmail": "hanako@example.com",
  "customerTel": "09012345678",
  "customerMessage": "お問い合わせ内容",
  "privacyAccepted": true
}
```

## Notes

- `privacyAccepted` は必須です。
- `inquiryOnly=true` の場合、予約日時・人数は必須ではありません。
- フロントエンドのテンプレートは `templates/index.html` が正本です。
- ルート直下の `index.html` は MVC 化のため不要なので、リポジトリから外します。
