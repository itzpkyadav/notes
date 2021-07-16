   public static boolean isNotificationEnabled(Context context) {
        boolean status = false;

        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            //Do your Work
            status = true;
        } else {
            //Ask for permission
            status = false;
        }

        return status;
    }
