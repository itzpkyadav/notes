public static String getTimeZone() {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        TimeZone zone = calendar.getTimeZone();
        return zone.getID();
    }
