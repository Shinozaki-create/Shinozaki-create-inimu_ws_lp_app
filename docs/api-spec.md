# API仕様

## GET /api/schedules

開催日と空席情報を返します。

```json
[
  {
    "date": "2026-06-06",
    "open": true,
    "totalCapacity": 30,
    "reservedCount": 4,
    "remainingCount": 26,
    "fullyBooked": false
  }
]
```

## GET /api/schedules/{date}/slots

指定日の時間枠を返します。`date` は `yyyy-MM-dd` 形式です。

```json
[
  {
    "slotId": 1,
    "startTime": "11:00",
    "endTime": "12:00",
    "capacity": 10,
    "reservedCount": 2,
    "remainingCount": 8,
    "active": true,
    "fullyBooked": false
  }
]
```

## POST /api/reservations

予約または問い合わせを登録します。`inquiryOnly=true` の場合は問い合わせとして保存されます。

予約:

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

問い合わせ:

```json
{
  "inquiryOnly": true,
  "customerFamilyName": "山田",
  "customerGivenName": "花子",
  "customerFamilyKana": "ヤマダ",
  "customerGivenKana": "ハナコ",
  "customerEmail": "hanako@example.com",
  "customerTel": "09012345678",
  "customerMessage": "開催内容について質問があります",
  "privacyAccepted": true
}
```

レスポンス:

```json
{
  "type": "reservation",
  "reservationCode": "WS-20260606-0001",
  "message": "予約を受け付けました"
}
```

```json
{
  "type": "inquiry",
  "reservationCode": null,
  "message": "お問い合わせを受け付けました"
}
```

## エラー

APIエラーはJSONで返します。

```json
{
  "message": "指定された時間枠は満席です"
}
```
