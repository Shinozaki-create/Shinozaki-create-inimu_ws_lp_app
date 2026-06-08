package com.example.inimuws.entity;

import com.example.inimuws.enums.InquiryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "inquiries")
public class Inquiry extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inquiry_code", nullable = false, unique = true)
    private String inquiryCode;

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

    @Column(name = "customer_message", nullable = false)
    private String customerMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InquiryStatus status;

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

    private static String toHiragana(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch >= '\u30A1' && ch <= '\u30F6') {
                builder.append((char) (ch - 0x60));
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }
}
