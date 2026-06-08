package com.example.inimuws.entity;

import com.example.inimuws.enums.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reservations")
public class Reservation extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_code", nullable = false, unique = true)
    private String reservationCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "time_slot_id", nullable = false)
    private WorkshopTimeSlot timeSlot;

    @Column(name = "reservation_date", nullable = false)
    private LocalDate reservationDate;

    @Column(name = "reservation_time", nullable = false)
    private LocalTime reservationTime;

    @Column(name = "reservation_count", nullable = false)
    private int reservationCount;

    @Column(name = "participant_count")
    private Integer participantCount;

    @Column(name = "male_count")
    private Integer maleCount;

    @Column(name = "female_count")
    private Integer femaleCount;

    @Column(name = "male_under_10_count")
    private Integer maleUnder10Count;

    @Column(name = "male_20s_count")
    private Integer male20sCount;

    @Column(name = "male_30s_count")
    private Integer male30sCount;

    @Column(name = "male_40s_count")
    private Integer male40sCount;

    @Column(name = "male_50s_count")
    private Integer male50sCount;

    @Column(name = "male_60_plus_count")
    private Integer male60PlusCount;

    @Column(name = "female_under_10_count")
    private Integer femaleUnder10Count;

    @Column(name = "female_20s_count")
    private Integer female20sCount;

    @Column(name = "female_30s_count")
    private Integer female30sCount;

    @Column(name = "female_40s_count")
    private Integer female40sCount;

    @Column(name = "female_50s_count")
    private Integer female50sCount;

    @Column(name = "female_60_plus_count")
    private Integer female60PlusCount;

    @Column(name = "customer_family_name", nullable = false)
    private String customerFamilyName;

    @Column(name = "customer_given_name", nullable = false)
    private String customerGivenName;

    @Column(name = "customer_family_kana", nullable = false)
    private String customerFamilyKana;

    @Column(name = "customer_given_kana", nullable = false)
    private String customerGivenKana;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(name = "customer_tel")
    private String customerTel;

    @Column(name = "customer_message")
    private String customerMessage;

    @Column(name = "total_amount", nullable = false)
    private int totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(name = "admin_memo")
    private String adminMemo;

    public String customerName() {
        return customerFamilyName + " " + customerGivenName;
    }

    public String customerFamilyHiragana() {
        return toHiragana(customerFamilyKana);
    }

    public String customerGivenHiragana() {
        return toHiragana(customerGivenKana);
    }

    public String customerFuriganaHiragana() {
        return customerFamilyHiragana() + " " + customerGivenHiragana();
    }

    public String formattedTotalAmount() {
        return NumberFormat.getNumberInstance(Locale.JAPAN).format(totalAmount);
    }

    public int effectiveParticipantCount() {
        if (participantCount != null) {
            return participantCount;
        }
        int ageBreakdownTotal = ageBreakdownTotal();
        if (ageBreakdownTotal > 0) {
            return ageBreakdownTotal;
        }
        int genderTotal = effectiveMaleCount() + effectiveFemaleCount();
        if (genderTotal > 0) {
            return genderTotal;
        }
        return reservationCount;
    }

    public int effectiveMaleCount() {
        return maleCount != null ? maleCount : maleAgeBreakdownTotal();
    }

    public int effectiveFemaleCount() {
        return femaleCount != null ? femaleCount : femaleAgeBreakdownTotal();
    }

    public int maleAgeBreakdownTotal() {
        return sum(
                maleUnder10Count,
                male20sCount,
                male30sCount,
                male40sCount,
                male50sCount,
                male60PlusCount
        );
    }

    public int femaleAgeBreakdownTotal() {
        return sum(
                femaleUnder10Count,
                female20sCount,
                female30sCount,
                female40sCount,
                female50sCount,
                female60PlusCount
        );
    }

    public int ageBreakdownTotal() {
        return maleAgeBreakdownTotal() + femaleAgeBreakdownTotal();
    }

    public boolean hasAgeBreakdown() {
        return maleUnder10Count != null
                || male20sCount != null
                || male30sCount != null
                || male40sCount != null
                || male50sCount != null
                || male60PlusCount != null
                || femaleUnder10Count != null
                || female20sCount != null
                || female30sCount != null
                || female40sCount != null
                || female50sCount != null
                || female60PlusCount != null;
    }

    private int sum(Integer... values) {
        int total = 0;
        for (Integer value : values) {
            if (value != null) {
                total += value;
            }
        }
        return total;
    }

    private static String toHiragana(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch >= 'ァ' && ch <= 'ヶ') {
                builder.append((char) (ch - 0x60));
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }
}
