package com.example.inimuws.controller.admin;

import com.example.inimuws.dto.DailySalesSummary;
import com.example.inimuws.dto.MonthlySalesSummary;
import com.example.inimuws.service.SalesService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/sales")
public class AdminSalesController {

    private final SalesService salesService;

    @GetMapping
    public String sales(@RequestParam(required = false) String month, Model model) {
        YearMonth targetMonth = parseMonth(month);
        model.addAttribute("targetMonth", targetMonth);
        model.addAttribute("dailySummaries", salesService.getDailySummaries(targetMonth));
        model.addAttribute("monthlySummary", salesService.getMonthlySummary(targetMonth));
        return "admin/sales";
    }

    @GetMapping("/csv")
    public void csv(@RequestParam(required = false) String month, HttpServletResponse response) throws IOException {
        YearMonth targetMonth = parseMonth(month);
        List<DailySalesSummary> rows = salesService.getDailySummaries(targetMonth);
        MonthlySalesSummary summary = salesService.getMonthlySummary(targetMonth);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=sales.csv");
        try (PrintWriter writer = response.getWriter()) {
            writer.println("日付,売上,予約件数,参加人数");
            for (DailySalesSummary row : rows) {
                writer.printf("%s,%d,%d,%d%n", row.date(), row.totalAmount(), row.reservationCount(), row.participantCount());
            }
            writer.printf("月合計,%d,%d,%d%n", summary.totalAmount(), summary.reservationCount(), summary.participantCount());
        }
    }

    private YearMonth parseMonth(String month) {
        if (!StringUtils.hasText(month)) {
            return YearMonth.now();
        }
        return YearMonth.parse(month);
    }
}
