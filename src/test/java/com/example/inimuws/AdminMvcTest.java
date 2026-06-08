package com.example.inimuws;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminLoginAuthenticatesInitialUser() throws Exception {
        mockMvc.perform(formLogin("/admin/login")
                        .user("email", "admin@example.com")
                        .password("password"))
                .andExpect(authenticated().withUsername("admin@example.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPagesRenderForAuthenticatedUser() throws Exception {
        assertOk("/admin");
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/admin/logout")))
                .andExpect(content().string(containsString("Workshop Admin")));

        assertOk("/admin/reservations");
        assertOk("/admin/reservations/new");
        assertOk("/admin/schedules");
        assertOk("/admin/sales");

        mockMvc.perform(get("/admin/inquiries"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("INQ-260502-01")));

        mockMvc.perform(get("/admin/inquiries/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("INQ-260502-01")));

        mockMvc.perform(get("/admin/schedules").param("month", "2026-06"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("11:00-12:00")))
                .andExpect(content().string(containsString("13:00-14:00")))
                .andExpect(content().string(containsString("15:00-16:00")))
                .andExpect(content().string(containsString("schedule-create-layout")))
                .andExpect(content().string(containsString("weekday-picker-grid")))
                .andExpect(content().string(containsString("<th>更新</th>")))
                .andExpect(content().string(containsString("<span>受付</span>")))
                .andExpect(content().string(containsString("data-schedule-target=\"schedule-2026-06-07\"")));

        mockMvc.perform(get("/admin/reservations/code").param("date", "2026-06-13"))
                .andExpect(status().isOk())
                .andExpect(content().string("WS-260613-03"));
    }

    @Test
    void publicHomepageIsRoutedThroughMvcAndTodoRoutesToAdmin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("inimu workshop")));

        mockMvc.perform(get("/index"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("inimu workshop")));

        mockMvc.perform(get("/todo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void loginLogoIsPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/admin/images/inimu-logo.png"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminLogoutRedirectsToLoginPage() throws Exception {
        mockMvc.perform(post("/admin/logout").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login?logout"));
    }

    void assertOk(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk());
    }
}
