package com.mopub.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import static android.content.Context.MODE_PRIVATE;

public final class SharedPreferencesHelper {
    public static final String DEFAULT_PREFERENCE_NAME = "mopubSettings";

    public interface CanGetPreferences {
        SharedPreferences getSharedPreferences(@NonNull final Context context, @NonNull final String preferenceName);
    }

    public static CanGetPreferences IMPL = new CanGetPreferences() {
        @Override
        public SharedPreferences getSharedPreferences(@NonNull Context context, @NonNull String preferenceName) {
            return context.getSharedPreferences(preferenceName, MODE_PRIVATE);
        }
    };


    private SharedPreferencesHelper() {}
    
    public static SharedPreferences getSharedPreferences(@NonNull final Context context) {
        return getSharedPreferences(context, DEFAULT_PREFERENCE_NAME);
    }

    public static SharedPreferences getSharedPreferences(
            @NonNull final Context context, @NonNull final String preferenceName) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(preferenceName);

        return IMPL.getSharedPreferences(context, preferenceName);
    }
}
