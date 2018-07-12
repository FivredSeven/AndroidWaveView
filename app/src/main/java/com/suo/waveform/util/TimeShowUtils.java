
package com.suo.waveform.util;

import android.text.TextUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 评论 时间日期显示
 */
public class TimeShowUtils {

    private static final long ONE_DAY_IN_MILLIS = 24 * 60 * 60 * 1000;
    private static final long ONE_HOUR_IN_MILLIS = 60 * 60 * 1000;
    private static final long ONE_MINUTE_IN_MILLIS = 60 * 1000;
    private static final long ONE_SECOND_IN_MILLIS = 1000;

    // 不带时分秒的日期格式
    private SimpleDateFormat formatWithoutHMS = null;

    private SimpleDateFormat formatWithoutHMSForKuqun = null;

    // 只有时分的格式
    private SimpleDateFormat formatWithHm = null;

    // 月份和日期 + 时分
    private SimpleDateFormat formatWithMDAndHm = null;

    // 年月日 时分秒
    SimpleDateFormat formatWithYMDAndHMS = null;

    public static final String FORMAT_YMD_WITH_HMS = "yyyy-MM-dd HH:mm:ss";

    private static TimeShowUtils instance = null;


    public static TimeShowUtils getInstance() {
        if (instance == null)
            instance = new TimeShowUtils();

        return instance;
    }

    public TimeShowUtils() {
        initDateFormat();
    }

    private void initDateFormat() {
        formatWithYMDAndHMS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        formatWithoutHMS = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
        formatWithMDAndHm = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA);
        formatWithHm = new SimpleDateFormat("HH:mm", Locale.CHINA);
        formatWithoutHMSForKuqun = new SimpleDateFormat("yy/M/dd", Locale.CHINA);
    }

    public String formatLongTime(long time, long sysTime) {
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        String sd = sdf.format(new Date(time));
        return formatCommentDateTime(sd,sysTime);
    }

    public String formatLongTime(String time) {
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        String sd = sdf.format(new Date(Long.parseLong(time)));
        return formatCommentDateTime(sd);
    }

    public String formatCommentDateTime(long publishTime) {
        return formatCommentDateTime(formatWithYMDAndHMS.format(publishTime), System.currentTimeMillis());
    }

    public String formatCommentDateTime(String publishTime) {
        return formatCommentDateTime(publishTime, System.currentTimeMillis());
    }

    public String formatCommentDateTime(String publishTime, long currentTime) {
        boolean isToday = isToday(publishTime, formatWithYMDAndHMS,currentTime);

        if (isToday) {  //当天
            long intervalTimeMillis = getIntervalTimeMillis(publishTime, formatWithYMDAndHMS, currentTime);
            if (intervalTimeMillis <= ONE_MINUTE_IN_MILLIS) { //1分钟内
                return "刚刚";
            } else if (intervalTimeMillis <= ONE_HOUR_IN_MILLIS) { //60分钟之内
                return intervalTimeMillis / ONE_MINUTE_IN_MILLIS + "分钟前";
            } else if (intervalTimeMillis <= ONE_DAY_IN_MILLIS) { //24小时之内
                return formatTime(formatWithHm, publishTime, FORMAT_YMD_WITH_HMS, currentTime);
            }
        } else if (isYesterday(publishTime, formatWithYMDAndHMS,currentTime)) {
            return "昨天 " + formatTime(formatWithHm, publishTime, FORMAT_YMD_WITH_HMS, currentTime);
        }
        return formatTime(null, publishTime, FORMAT_YMD_WITH_HMS, currentTime);
    }

    public String formatCommentDateTimeForKuqun(String publishTime) throws ParseException {

        long currentTime = System.currentTimeMillis();
        boolean isToday = isToday(publishTime, formatWithYMDAndHMS);

        if (isToday) {  //当天
            long intervalTimeMillis = getIntervalTimeMillis(publishTime, formatWithYMDAndHMS, currentTime);
            if (intervalTimeMillis <= ONE_MINUTE_IN_MILLIS) { //1分钟内
                return "刚刚";
            } else if (intervalTimeMillis <= ONE_HOUR_IN_MILLIS) { //60分钟之内
                return intervalTimeMillis / ONE_MINUTE_IN_MILLIS + "分钟前";
            } else if (intervalTimeMillis <= ONE_DAY_IN_MILLIS) { //24小时之内
                return formatTime(formatWithHm, publishTime, FORMAT_YMD_WITH_HMS, currentTime);
            }
        } else if (isYesterday(publishTime, formatWithYMDAndHMS)) {
            return "昨天";
        }
        return formatWithoutHMSForKuqun.format(formatWithYMDAndHMS.parse(publishTime));  // 16/7/25
    }

    public boolean isToday(String publishTime, SimpleDateFormat dateFormat)
    {
        // 用用户手机的当前时间
        return isToday(publishTime,dateFormat, System.currentTimeMillis());
    }

    public boolean isToday(String publishTime, SimpleDateFormat dateFormat, long currentTimeMillis) {

        try {
            Date phoneDate = formatWithoutHMS.parse(formatWithoutHMS.format(currentTimeMillis));

            long todayBeginTimeInMillis = phoneDate.getTime();

            long publishTimeLong = dateFormat.parse(publishTime).getTime();

            if (publishTimeLong - todayBeginTimeInMillis < ONE_DAY_IN_MILLIS) {
                if (publishTimeLong >= todayBeginTimeInMillis) {
                    return true;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isYesterday(String publishTime, SimpleDateFormat dateFormat){
        // 用用户手机的当前时间
        return isYesterday(publishTime,dateFormat, System.currentTimeMillis());
    }

    public boolean isYesterday(String publishTime, SimpleDateFormat dateFormat, long currentTimeMillis) {
        try {
            Date phoneDate = formatWithoutHMS.parse(formatWithoutHMS.format(currentTimeMillis));
            int systemYear = phoneDate.getYear();

            long todayBeginTimeInMillis = phoneDate.getTime();

            long yesterdayBeginTimeInMillis = phoneDate.getTime() - ONE_DAY_IN_MILLIS;

            long publishTimeLong = dateFormat.parse(publishTime).getTime();

            int publishYear = formatWithoutHMS.parse(formatWithoutHMS.format(publishTimeLong)).getYear();

            if (systemYear != publishYear) {
                return false;
            }

            if (publishTimeLong < todayBeginTimeInMillis && publishTimeLong >= yesterdayBeginTimeInMillis) {
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public long getIntervalTimeMillis(String publishTime, SimpleDateFormat dateFormat, long currentTime) {

        if (dateFormat == null || TextUtils.isEmpty(publishTime)) {
            return Long.MAX_VALUE;
        }

        try {
            long publishTime2Long = dateFormat.parse(publishTime).getTime();
            if (currentTime >= publishTime2Long) {
                return (currentTime - publishTime2Long);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Long.MAX_VALUE;
    }


    public String formatTime(SimpleDateFormat format, String publishTime, String formatStr, long currentTime) {
        String date = "";
        if (TextUtils.isEmpty(formatStr)) {
            return date;
        }

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(formatStr, Locale.CHINA);
            long publishTime2Long = dateFormat.parse(publishTime).getTime();
            // 用户手机的当前时间
            Date phoneDate = dateFormat.parse(dateFormat.format(currentTime));
            date = formatTime(format, publishTime2Long, phoneDate);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return date;

    }

    private String formatTime(SimpleDateFormat format, long publishTimeLong, Date phoneDate) {
        String date = "";

        try {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(publishTimeLong);
            int targetYear = cal.get(Calendar.YEAR);
            cal.setTime(phoneDate);
            int thisYear = cal.get(Calendar.YEAR);

            if (thisYear - targetYear == 0) {
                if (format != null)
                    date = format.format(publishTimeLong);
                else
                    date = formatWithMDAndHm.format(publishTimeLong);
            } else {
                date = formatWithoutHMS.format(publishTimeLong);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date;
    }

    public static String getDurationFromTime(int time) {
        if (time <= 0) {
            return "未知";
        }
        int min = time / 1000 / 60;
        int sec = time / 1000 % 60;
        return (min < 10 ? "0"+min : min) + ":" + (sec < 10 ? "0"+sec : sec);
    }
}
