package com.example.inimuws.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReservationRequest {

    @JsonAlias("inquiry_only")
    private Boolean inquiryOnly = false;

    @JsonAlias("reservation_date")
    private LocalDate reservationDate;

    @JsonAlias("reservation_time")
    private LocalTime reservationTime;

    @JsonAlias("reservation_count")
    private Integer reservationCount;

    @NotBlank(message = "姓を入力してください")
    @JsonAlias("customer_family_name")
    private String customerFamilyName;

    @NotBlank(message = "名を入力してください")
    @JsonAlias("customer_given_name")
    private String customerGivenName;

    @NotBlank(message = "姓カナを入力してください")
    @JsonAlias("customer_family_kana")
    private String customerFamilyKana;

    @NotBlank(message = "名カナを入力してください")
    @JsonAlias("customer_given_kana")
    private String customerGivenKana;

    @NotBlank(message = "メールアドレスを入力してください")
    @Email(message = "メールアドレスの形式が正しくありません")
    @JsonAlias("customer_email")
    private String customerEmail;

    @JsonAlias("customer_tel")
    private String customerTel;

    @JsonAlias("customer_message")
    private String customerMessage;

    @AssertTrue(message = "プライバシーポリシーに同意してください")
    @JsonAlias("privacy")
    private Boolean privacyAccepted;

    public boolean isInquiryOnly() {
        return Boolean.TRUE.equals(inquiryOnly);
    }
}
