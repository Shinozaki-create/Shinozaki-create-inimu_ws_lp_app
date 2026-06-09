package com.example.inimuws;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PublicApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void schedulesApiReturnsSchedules() throws Exception {
        mockMvc.perform(get("/api/schedules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value("2026-05-02"));
    }

    @Test
    void slotsApiReturnsSlots() throws Exception {
        mockMvc.perform(get("/api/schedules/2026-06-06/slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].startTime").value("11:00"))
                .andExpect(jsonPath("$[1].startTime").value("13:00"))
                .andExpect(jsonPath("$[1].remainingCount").value(2))
                .andExpect(jsonPath("$[1].fullyBooked").value(false))
                .andExpect(jsonPath("$[2].startTime").value("15:00"))
                .andExpect(jsonPath("$[2].remainingCount").value(0))
                .andExpect(jsonPath("$[2].fullyBooked").value(true));
    }

    @Test
    void reservationApiAcceptsCamelCaseJson() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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
                                  "customerMessage": "2名で予約します。",
                                  "privacyAccepted": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("reservation"))
                .andExpect(jsonPath("$.reservationCode", startsWith("WS-260606-")));
    }

    @Test
    void reservationApiAcceptsSnakeCaseJsonForFrontendForms() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inquiry_only": true,
                                  "customer_family_name": "山田",
                                  "customer_given_name": "花子",
                                  "customer_family_kana": "ヤマダ",
                                  "customer_given_kana": "ハナコ",
                                  "customer_email": "hanako@example.com",
                                  "customer_tel": "09012345678",
                                  "customer_message": "日程について教えてください。",
                                  "privacy": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("inquiry"));
    }
}
