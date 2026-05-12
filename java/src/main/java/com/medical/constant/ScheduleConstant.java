package com.medical.constant;

public class ScheduleConstant {

    public static final String SHIFT_MORNING = "morning";
    public static final String SHIFT_AFTERNOON = "afternoon";
    public static final String SHIFT_EVENING = "evening";

    public static final String SHIFT_MORNING_DESC = "早班";
    public static final String SHIFT_AFTERNOON_DESC = "中班";
    public static final String SHIFT_EVENING_DESC = "晚班";

    public static final String TIME_MORNING_START = "08:00";
    public static final String TIME_MORNING_END = "14:00";
    public static final String TIME_AFTERNOON_START = "14:00";
    public static final String TIME_AFTERNOON_END = "20:00";
    public static final String TIME_EVENING_START = "20:00";
    public static final String TIME_EVENING_END = "08:00";

    public static final Integer STATUS_AVAILABLE = 1;
    public static final Integer STATUS_FULL = 2;
    public static final Integer STATUS_REST = 0;

    public static final Integer ON_DUTY = 1;
    public static final Integer OFF_DUTY = 0;

    public static final Integer SCHEDULE_STATUS_ACTIVE = 1;
    public static final Integer SCHEDULE_STATUS_INACTIVE = 0;

    public static int getShiftOrder(String shiftType) {
        return switch (shiftType) {
            case SHIFT_MORNING -> 1;
            case SHIFT_AFTERNOON -> 2;
            case SHIFT_EVENING -> 3;
            default -> 0;
        };
    }

    public static String getShiftDescription(String shiftType) {
        return switch (shiftType) {
            case SHIFT_MORNING -> SHIFT_MORNING_DESC;
            case SHIFT_AFTERNOON -> SHIFT_AFTERNOON_DESC;
            case SHIFT_EVENING -> SHIFT_EVENING_DESC;
            default -> shiftType;
        };
    }

    public static String getShiftTimeRange(String shiftType) {
        return switch (shiftType) {
            case SHIFT_MORNING -> TIME_MORNING_START + "-" + TIME_MORNING_END;
            case SHIFT_AFTERNOON -> TIME_AFTERNOON_START + "-" + TIME_AFTERNOON_END;
            case SHIFT_EVENING -> TIME_EVENING_START + "-" + TIME_EVENING_END;
            default -> "";
        };
    }
}