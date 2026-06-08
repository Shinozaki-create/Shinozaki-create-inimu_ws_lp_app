# Frontend Integration

公開フロントエンドは Spring Boot の `/` で配信します。

## 配置

- `index.html`
  - `src/main/resources/templates/index.html`
- `assets/css/style.css`
  - `src/main/resources/static/assets/css/style.css`
- `assets/js/script.js`
  - `src/main/resources/static/assets/js/script.js`
- 画像素材
  - `src/main/resources/static/assets/img/**`
  - `src/main/resources/static/assets/icons/**`

## Public API

- `GET /api/schedules`
- `GET /api/schedules/{date}/slots`
- `POST /api/reservations`

## Frontend Flow

1. ページ表示時に `GET /api/schedules` を呼び出し、開催日カレンダーを描画します。
2. 日付を選ぶと `GET /api/schedules/{date}/slots` を呼び出し、時間帯と残席数を更新します。
3. 予約フォームの送信前に確認画面を表示します。
4. 確認後に `POST /api/reservations` を送信します。
5. 送信成功時はレスポンスメッセージをそのまま表示します。

## Request Body

バックエンドは camelCase と snake_case の両方を受け付けます。

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
  "customerMessage": "友人と参加します",
  "privacyAccepted": true
}
```

問い合わせのみの場合:

```json
{
  "inquiryOnly": true,
  "customerFamilyName": "山田",
  "customerGivenName": "花子",
  "customerFamilyKana": "ヤマダ",
  "customerGivenKana": "ハナコ",
  "customerEmail": "hanako@example.com",
  "customerTel": "09012345678",
  "customerMessage": "空き状況について質問です",
  "privacyAccepted": true
}
```

## Response Body

### `GET /api/schedules`

```json
[
  {
    "date": "2026-06-06",
    "open": true,
    "totalCapacity": 30,
    "reservedCount": 0,
    "remainingCount": 30,
    "fullyBooked": false
  }
]
```

### `GET /api/schedules/{date}/slots`

```json
[
  {
    "slotId": 1,
    "startTime": "11:00",
    "endTime": "12:00",
    "capacity": 10,
    "reservedCount": 0,
    "remainingCount": 10,
    "active": true,
    "fullyBooked": false
  }
]
```

### `POST /api/reservations`

```json
{
  "type": "reservation",
  "reservationCode": "WS-260606-01",
  "message": "予約を受け付けました"
}
```

問い合わせのみの場合は `type: "inquiry"` になります。

## Error Response

```json
{
  "message": "入力内容を確認してください"
}
```

## Notes

- フロントエンドは同一オリジンで API を呼び出すため、通常は CORS 設定を意識しません。
- `privacyAccepted` は必須です。
- `inquiryOnly=true` の場合は予約日時・時間・人数の入力は不要です。
