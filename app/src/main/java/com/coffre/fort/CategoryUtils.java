package com.coffre.fort;

import android.content.Context;

import java.util.Arrays;
import java.util.List;

public final class CategoryUtils {

    private CategoryUtils() {
    }

    public static boolean isMessageCategory(Context context, String category) {
        if (category == null) {
            return false;
        }
        String combined = context.getString(R.string.category_messages);
        String legacySms = context.getString(R.string.category_sms);
        return category.equals(combined) || category.equals(legacySms);
    }

    public static String normalizeCategory(Context context, String category) {
        if (isMessageCategory(context, category)) {
            return context.getString(R.string.category_messages);
        }
        return category;
    }

    public static List<String> getMessageCategoriesForQuery(Context context) {
        return Arrays.asList(
                context.getString(R.string.category_messages),
                context.getString(R.string.category_sms)
        );
    }
}
