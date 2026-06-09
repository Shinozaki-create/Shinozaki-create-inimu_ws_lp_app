INSERT INTO admin_users (id, email, password_hash, name, role, enabled, created_at, updated_at)
VALUES
    (1, 'admin@example.com', '$2a$10$LIFGFmvo.8aW/sYZLpFxLuJUrAAPLllbNx3OKe2ZV9rgDrQL/rTQW', '管理者', 'ADMIN', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO system_settings (id, setting_key, setting_value, description, created_at, updated_at)
VALUES
    (1, 'price_per_person', '5500', '1名あたりの参加料金', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO workshop_schedules (schedule_date, is_open, note, created_at, updated_at)
SELECT calendar_days.d,
       TRUE,
       CASE WHEN holiday_dates.d IS NOT NULL THEN '祝日開催' ELSE NULL END,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM (
         SELECT DATEADD('DAY', day_index.X, DATE '2026-05-01') AS d
         FROM SYSTEM_RANGE(0, 152) AS day_index
     ) calendar_days
         LEFT JOIN (
    VALUES
        (DATE '2026-05-03'),
        (DATE '2026-05-04'),
        (DATE '2026-05-05'),
        (DATE '2026-05-06'),
        (DATE '2026-07-20'),
        (DATE '2026-08-11'),
        (DATE '2026-09-21'),
        (DATE '2026-09-22'),
        (DATE '2026-09-23')
) AS holiday_dates(d) ON holiday_dates.d = calendar_days.d
WHERE MOD(DATEDIFF('DAY', DATE '2026-05-01', calendar_days.d), 7) IN (1, 2)
   OR holiday_dates.d IS NOT NULL;

INSERT INTO workshop_time_slots (
    schedule_id,
    start_time,
    end_time,
    capacity,
    reserved_count,
    is_active,
    created_at,
    updated_at
)
SELECT s.id, TIME '11:00:00', TIME '12:00:00', 10, 0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM workshop_schedules s
WHERE s.schedule_date BETWEEN DATE '2026-05-01' AND DATE '2026-09-30';

INSERT INTO workshop_time_slots (
    schedule_id,
    start_time,
    end_time,
    capacity,
    reserved_count,
    is_active,
    created_at,
    updated_at
)
SELECT s.id, TIME '13:00:00', TIME '14:00:00', 10, 0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM workshop_schedules s
WHERE s.schedule_date BETWEEN DATE '2026-05-01' AND DATE '2026-09-30';

INSERT INTO workshop_time_slots (
    schedule_id,
    start_time,
    end_time,
    capacity,
    reserved_count,
    is_active,
    created_at,
    updated_at
)
SELECT s.id, TIME '15:00:00', TIME '16:00:00', 10, 0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM workshop_schedules s
WHERE s.schedule_date BETWEEN DATE '2026-05-01' AND DATE '2026-09-30';

INSERT INTO reservations (
    reservation_code,
    time_slot_id,
    reservation_date,
    reservation_time,
    reservation_count,
    customer_family_name,
    customer_given_name,
    customer_family_kana,
    customer_given_kana,
    customer_email,
    customer_tel,
    customer_message,
    total_amount,
    status,
    admin_memo,
    created_at,
    updated_at
)
SELECT
    seed.reservation_code,
    ts.id,
    seed.reservation_date,
    seed.reservation_time,
    seed.reservation_count,
    seed.customer_family_name,
    seed.customer_given_name,
    seed.customer_family_kana,
    seed.customer_given_kana,
    seed.customer_email,
    seed.customer_tel,
    seed.customer_message,
    seed.reservation_count * 5500,
    seed.status,
    seed.admin_memo,
    seed.created_at,
    seed.updated_at
FROM (
    VALUES
        ('WS-260503-01', DATE '2026-05-03', TIME '11:00:00', 2, '佐藤', '美咲', 'サトウ', 'ミサキ', 'misaki.sato@example.com', '09011112222', '親子で参加します。', 'PENDING', NULL, TIMESTAMP '2026-05-01 09:30:00', TIMESTAMP '2026-05-01 09:30:00'),
        ('WS-260503-02', DATE '2026-05-03', TIME '13:00:00', 3, '鈴木', '太郎', 'スズキ', 'タロウ', 'taro.suzuki@example.com', '09022223333', '友人と参加予定です。', 'CONFIRMED', '事前案内送付', TIMESTAMP '2026-05-01 10:15:00', TIMESTAMP '2026-05-01 10:40:00'),
        ('WS-260510-01', DATE '2026-05-10', TIME '15:00:00', 4, '田中', '彩', 'タナカ', 'アヤ', 'aya.tanaka@example.com', '09033334444', '家族4名で参加します。', 'COMPLETED', NULL, TIMESTAMP '2026-05-02 13:20:00', TIMESTAMP '2026-05-02 14:00:00'),
        ('WS-260524-01', DATE '2026-05-24', TIME '11:00:00', 2, '高橋', '健', 'タカハシ', 'ケン', 'ken.takahashi@example.com', '09044445555', 'キャンセル待ちの連絡です。', 'CANCELLED', '空席再調整', TIMESTAMP '2026-05-08 17:05:00', TIMESTAMP '2026-05-08 17:40:00'),
        ('WS-260606-01', DATE '2026-06-06', TIME '13:00:00', 8, '見本', '花', 'ミホン', 'ハナ', 'sample.low@example.com', '09012340001', '残り僅か表示用のサンプルです。', 'CONFIRMED', 'public calendar demo data', TIMESTAMP '2026-05-15 10:00:00', TIMESTAMP '2026-05-15 10:30:00'),
        ('WS-260606-02', DATE '2026-06-06', TIME '15:00:00', 10, '見本', '満', 'ミホン', 'ミツル', 'sample.full@example.com', '09012340002', '満席表示用のサンプルです。', 'CONFIRMED', 'public calendar demo data', TIMESTAMP '2026-05-15 11:00:00', TIMESTAMP '2026-05-15 11:30:00'),
        ('WS-260607-01', DATE '2026-06-07', TIME '11:00:00', 2, '伊藤', '葵', 'イトウ', 'アオイ', 'aoi.ito@example.com', '09055556666', '初心者ですが参加できますか。', 'PENDING', NULL, TIMESTAMP '2026-05-18 08:50:00', TIMESTAMP '2026-05-18 08:50:00'),
        ('WS-260607-02', DATE '2026-06-07', TIME '15:00:00', 3, '中村', '悠', 'ナカムラ', 'ユウ', 'yu.nakamura@example.com', '09066667777', '親子で参加予定です。', 'CONFIRMED', '同伴者把握', TIMESTAMP '2026-05-18 09:10:00', TIMESTAMP '2026-05-18 09:45:00'),
        ('WS-260613-01', DATE '2026-06-13', TIME '11:00:00', 3, '小林', '真奈', 'コバヤシ', 'マナ', 'mana.kobayashi@example.com', '09077778888', '持ち物を確認したいです。', 'COMPLETED', NULL, TIMESTAMP '2026-05-20 10:25:00', TIMESTAMP '2026-05-20 11:05:00'),
        ('WS-260613-02', DATE '2026-06-13', TIME '13:00:00', 4, '渡辺', '亮', 'ワタナベ', 'リョウ', 'ryo.watanabe@example.com', '09088889999', '会社の同僚と参加します。', 'CONFIRMED', '参加者情報整理', TIMESTAMP '2026-05-20 13:35:00', TIMESTAMP '2026-05-20 14:00:00'),
        ('WS-260627-01', DATE '2026-06-27', TIME '15:00:00', 2, '山本', '咲', 'ヤマモト', 'サキ', 'saki.yamamoto@example.com', '09099990000', '当日連絡が遅れるかもしれません。', 'NO_SHOW', '不在記録', TIMESTAMP '2026-05-24 12:00:00', TIMESTAMP '2026-05-24 12:20:00'),
        ('WS-260720-01', DATE '2026-07-20', TIME '11:00:00', 2, '斎藤', '遥', 'サイトウ', 'ハルカ', 'haruka.saito@example.com', '08011112222', '夏休みの予定で参加します。', 'PENDING', NULL, TIMESTAMP '2026-06-05 09:00:00', TIMESTAMP '2026-06-05 09:00:00'),
        ('WS-260720-02', DATE '2026-07-20', TIME '13:00:00', 3, '木村', '陽菜', 'キムラ', 'ヒナ', 'hina.kimura@example.com', '08022223333', '友人と2組で参加します。', 'CONFIRMED', '参加確認', TIMESTAMP '2026-06-05 10:10:00', TIMESTAMP '2026-06-05 10:35:00'),
        ('WS-260726-01', DATE '2026-07-26', TIME '15:00:00', 2, '松本', '颯', 'マツモト', 'ハヤテ', 'hayate.matsumoto@example.com', '08033334444', '午後の回を希望します。', 'COMPLETED', NULL, TIMESTAMP '2026-06-08 11:45:00', TIMESTAMP '2026-06-08 12:10:00'),
        ('WS-260811-01', DATE '2026-08-11', TIME '11:00:00', 1, '井上', '莉子', 'イノウエ', 'リコ', 'riko.inoue@example.com', '08044445555', '1名での参加です。', 'PENDING', NULL, TIMESTAMP '2026-06-18 08:15:00', TIMESTAMP '2026-06-18 08:15:00'),
        ('WS-260811-02', DATE '2026-08-11', TIME '13:00:00', 4, '清水', '大輝', 'シミズ', 'ダイキ', 'daiki.shimizu@example.com', '08055556666', '家族で夏休み参加です。', 'CONFIRMED', '家族参加把握', TIMESTAMP '2026-06-18 09:35:00', TIMESTAMP '2026-06-18 10:00:00'),
        ('WS-260823-01', DATE '2026-08-23', TIME '15:00:00', 3, '山田', '花子', 'ヤマダ', 'ハナコ', 'hanako.yamada@example.com', '08066667777', '再参加です。', 'CONFIRMED', '紹介経由記録', TIMESTAMP '2026-06-25 15:20:00', TIMESTAMP '2026-06-25 15:50:00'),
        ('WS-260921-01', DATE '2026-09-21', TIME '11:00:00', 2, '近藤', '結衣', 'コンドウ', 'ユイ', 'yui.kondo@example.com', '08077778888', '敬老の日の連休に参加します。', 'PENDING', NULL, TIMESTAMP '2026-07-12 09:10:00', TIMESTAMP '2026-07-12 09:10:00'),
        ('WS-260921-02', DATE '2026-09-21', TIME '13:00:00', 3, '石川', '拓海', 'イシカワ', 'タクミ', 'takumi.ishikawa@example.com', '08088889999', '紹介で申し込みました。', 'CONFIRMED', '申込経路記録', TIMESTAMP '2026-07-12 09:45:00', TIMESTAMP '2026-07-12 10:20:00'),
        ('WS-260922-01', DATE '2026-09-22', TIME '15:00:00', 2, '前田', '真央', 'マエダ', 'マオ', 'mao.maeda@example.com', '08099990000', '予定変更のためキャンセルします。', 'CANCELLED', '再募集対応', TIMESTAMP '2026-07-12 11:00:00', TIMESTAMP '2026-07-12 11:35:00'),
        ('WS-260923-01', DATE '2026-09-23', TIME '11:00:00', 5, '森', '樹', 'モリ', 'イツキ', 'itsuki.mori@example.com', '08111112222', '祝日にまとめて参加します。', 'COMPLETED', NULL, TIMESTAMP '2026-07-12 12:10:00', TIMESTAMP '2026-07-12 12:40:00'),
        ('WS-260927-01', DATE '2026-09-27', TIME '13:00:00', 2, '小川', '紬', 'オガワ', 'ツムギ', 'tsumugi.ogawa@example.com', '08122223333', '月末の回を予約します。', 'PENDING', NULL, TIMESTAMP '2026-07-12 13:25:00', TIMESTAMP '2026-07-12 13:25:00')
) AS seed(
    reservation_code,
    reservation_date,
    reservation_time,
    reservation_count,
    customer_family_name,
    customer_given_name,
    customer_family_kana,
    customer_given_kana,
    customer_email,
    customer_tel,
    customer_message,
    status,
    admin_memo,
    created_at,
    updated_at
)
JOIN workshop_schedules s ON s.schedule_date = seed.reservation_date
JOIN workshop_time_slots ts ON ts.schedule_id = s.id AND ts.start_time = seed.reservation_time;

UPDATE workshop_time_slots ts
SET reserved_count = COALESCE((
    SELECT SUM(r.reservation_count)
    FROM reservations r
    WHERE r.time_slot_id = ts.id
      AND r.status <> 'CANCELLED'
), 0);

INSERT INTO inquiries (
    inquiry_code,
    customer_family_name,
    customer_given_name,
    customer_family_kana,
    customer_given_kana,
    customer_email,
    customer_tel,
    customer_message,
    status,
    admin_memo,
    created_at,
    updated_at
)
VALUES
    ('INQ-260502-01', '中村', '悠', 'ナカムラ', 'ユウ', 'yu.nakamura.inquiry@example.com', '09011119999', '小学生でも参加できますか。', 'OPEN', NULL, TIMESTAMP '2026-05-02 08:40:00', TIMESTAMP '2026-05-02 08:40:00'),
    ('INQ-260518-01', '小林', '真奈', 'コバヤシ', 'マナ', 'mana.kobayashi.inquiry@example.com', '09022220000', '持ち物を教えてください。', 'IN_PROGRESS', '返信文面を作成中', TIMESTAMP '2026-05-18 12:05:00', TIMESTAMP '2026-05-18 12:30:00'),
    ('INQ-260603-01', '渡辺', '亮', 'ワタナベ', 'リョウ', 'ryo.watanabe.inquiry@example.com', '09033331111', '法人参加は可能ですか。', 'CLOSED', 'メール返信済み', TIMESTAMP '2026-06-03 16:25:00', TIMESTAMP '2026-06-03 17:10:00'),
    ('INQ-260701-01', '森', '彩', 'モリ', 'アヤ', 'aya.mori.inquiry@example.com', '09044442222', '夏休みの開催日程を知りたいです。', 'OPEN', NULL, TIMESTAMP '2026-07-01 09:05:00', TIMESTAMP '2026-07-01 09:05:00'),
    ('INQ-260722-01', '田中', '健', 'タナカ', 'ケン', 'ken.tanaka.inquiry@example.com', '09055553333', '送迎の必要はありますか。', 'IN_PROGRESS', '確認中', TIMESTAMP '2026-07-22 11:20:00', TIMESTAMP '2026-07-22 11:55:00'),
    ('INQ-260804-01', '高橋', '美咲', 'タカハシ', 'ミサキ', 'misaki.takahashi.inquiry@example.com', '09066664444', '小学生の兄弟でも一緒に参加できますか。', 'OPEN', NULL, TIMESTAMP '2026-08-04 14:10:00', TIMESTAMP '2026-08-04 14:10:00'),
    ('INQ-260818-01', '鈴木', '葵', 'スズキ', 'アオイ', 'aoi.suzuki.inquiry@example.com', '09077775555', '材料は何を使いますか。', 'CLOSED', '案内済み', TIMESTAMP '2026-08-18 10:35:00', TIMESTAMP '2026-08-18 11:05:00'),
    ('INQ-260909-01', '佐藤', '遥', 'サトウ', 'ハルカ', 'haruka.sato.inquiry@example.com', '09088886666', '9月の空き状況を教えてください。', 'OPEN', NULL, TIMESTAMP '2026-09-09 08:55:00', TIMESTAMP '2026-09-09 08:55:00'),
    ('INQ-260924-01', '木村', '亮', 'キムラ', 'リョウ', 'ryo.kimura.inquiry@example.com', '09099997777', '団体予約は何名から可能ですか。', 'IN_PROGRESS', '返答待ち', TIMESTAMP '2026-09-24 13:40:00', TIMESTAMP '2026-09-24 14:05:00'),
    ('INQ-260928-01', '山本', '結衣', 'ヤマモト', 'ユイ', 'yui.yamamoto.inquiry@example.com', '09111118888', 'キャンセル期限を確認したいです。', 'OPEN', NULL, TIMESTAMP '2026-09-28 17:15:00', TIMESTAMP '2026-09-28 17:15:00');

