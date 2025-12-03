package com.gitranker.api.infrastructure.github.util;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DateRangeUtils {
    public static List<DateRange> splitByYears(LocalDate startDate, LocalDate endDate) {
        List<DateRange> ranges = new ArrayList<>();

        LocalDate currentStart = startDate;
        endDate = endDate.minusDays(1);

        while (currentStart.isBefore(endDate)) {
            LocalDate currentEnd = currentStart.plusYears(1).minusDays(1);

            if (currentEnd.isAfter(endDate)) {
                currentEnd = endDate;
            }

            ranges.add(new DateRange(currentStart, currentEnd));
            currentStart = currentEnd.plusDays(1);
        }
        return ranges;
    }

    public record DateRange(LocalDate start, LocalDate end) {
        public String toGitHubQueryFormat() {
            return start + ".." + end;
        }
    }
}
