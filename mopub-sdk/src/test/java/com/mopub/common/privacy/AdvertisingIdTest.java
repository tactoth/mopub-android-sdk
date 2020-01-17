// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.privacy;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.TimeZone;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
public class AdvertisingIdTest {
    private static final long ONE_DAY_MS = 24 * 60 * 60 * 1000;
    private static final long TEN_SECONDS_MS = 10 * 1000;

    private static final String MOPUB_ID = "test-id-mopub";
    private static final String ANDROID_ID = "test-id-android";

    private AdvertisingId subject;
    private Calendar time;
    private long now;

    @Before
    public void setup() {
        time = Calendar.getInstance();
        now = time.getTimeInMillis();
    }

    @Test
    public void constructor_shouldInitializeCorrectly() {
        subject = new AdvertisingId(ANDROID_ID, MOPUB_ID, false, now);
        assertThat(subject.mAdvertisingId).isEqualTo(ANDROID_ID);
        assertThat(subject.mMopubId).isEqualTo(MOPUB_ID);
        assertThat(subject.mDoNotTrack).isFalse();
        assertThat(subject.mLastRotation).isEqualTo(time);

        subject = new AdvertisingId(ANDROID_ID, MOPUB_ID, true, now);
        assertThat(subject.mDoNotTrack).isTrue();
        // return IFA even when DoNotTrack is true
        assertThat(subject.getIfaWithPrefix()).isEqualTo("ifa:" + ANDROID_ID);
        assertThat(subject.mLastRotation.get(Calendar.DAY_OF_YEAR)).isEqualTo(
                time.get(Calendar.DAY_OF_YEAR));
    }

    @Test
    public void isRotationRequired_whenMoreThan24Hours_shouldReturnTrue() {
        // one day and ten seconds ago
        subject = new AdvertisingId(ANDROID_ID, MOPUB_ID, false, now - ONE_DAY_MS - TEN_SECONDS_MS);
        assertThat(subject.isRotationRequired()).isTrue();
    }

    @Test
    public void isRotationRequired_whenMidnightSameDay_shouldReturnFalse() {
        final Calendar sameDayCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        sameDayCalendar.set(sameDayCalendar.get(Calendar.YEAR), sameDayCalendar.get(Calendar.MONTH),
                sameDayCalendar.get(Calendar.DAY_OF_MONTH), 0, 0, 0);

        subject = new AdvertisingId(ANDROID_ID, MOPUB_ID, false,
                sameDayCalendar.getTimeInMillis());

        assertThat(subject.isRotationRequired()).isFalse();
    }

    @Test
    public void isRotationRequired_whenLastSecondOfTheSameDay_shouldReturnFalse() {
        final Calendar sameDayCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        sameDayCalendar.set(sameDayCalendar.get(Calendar.YEAR), sameDayCalendar.get(Calendar.MONTH),
                sameDayCalendar.get(Calendar.DAY_OF_MONTH), 23, 59, 59);

        subject = new AdvertisingId(ANDROID_ID, MOPUB_ID, false,
                sameDayCalendar.getTimeInMillis());

        assertThat(subject.isRotationRequired()).isFalse();
    }
    @Test
    public void isRotationRequired_whenMidnightOfNextDay_shouldReturnTrue() {
        final Calendar sameDayCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        sameDayCalendar.set(sameDayCalendar.get(Calendar.YEAR), sameDayCalendar.get(Calendar.MONTH),
                sameDayCalendar.get(Calendar.DAY_OF_MONTH), 23, 59, 59);
        sameDayCalendar.add(Calendar.SECOND, 1);

        subject = new AdvertisingId(ANDROID_ID, MOPUB_ID, false,
                sameDayCalendar.getTimeInMillis());

        assertThat(subject.isRotationRequired()).isTrue();
    }

    @Test
    public void isRotationRequired_whenTimeZoneCausesDayDifference_shouldReturnTrue() {
        final Calendar sameDayCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        sameDayCalendar.set(sameDayCalendar.get(Calendar.YEAR), sameDayCalendar.get(Calendar.MONTH),
                sameDayCalendar.get(Calendar.DAY_OF_MONTH), 5, 0, 0);
        sameDayCalendar.setTimeZone(TimeZone.getTimeZone("PST"));
        sameDayCalendar.add(Calendar.DATE, -1);

        subject = new AdvertisingId(ANDROID_ID, MOPUB_ID, false,
                sameDayCalendar.getTimeInMillis());

        assertThat(subject.isRotationRequired()).isTrue();
    }

    @Test
    public void isRotationRequired_whenTimeZoneCausesNoDayDifference_shouldReturnFalse() {
        final Calendar sameDayCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        sameDayCalendar.set(sameDayCalendar.get(Calendar.YEAR), sameDayCalendar.get(Calendar.MONTH),
                sameDayCalendar.get(Calendar.DAY_OF_MONTH), 22, 0, 0);
        sameDayCalendar.setTimeZone(TimeZone.getTimeZone("PST"));
        sameDayCalendar.add(Calendar.DATE, -1);

        subject = new AdvertisingId(ANDROID_ID, MOPUB_ID, false,
                sameDayCalendar.getTimeInMillis());

        assertThat(subject.isRotationRequired()).isFalse();
    }

    @Test
    public void getIdWithPrefix_whenDoNotTrackFalse_shouldReturnIfaString() {
        subject = new AdvertisingId(ANDROID_ID, MOPUB_ID, false, now);
        assertThat(subject.getIdWithPrefix(true)).isEqualTo("ifa:" + ANDROID_ID);
    }

    @Test
    public void getIdWithPrefix_whenAndroidIdUnavailable_shouldReturnMopubString() {
        subject = new AdvertisingId("", MOPUB_ID, false, now);
        assertThat(subject.getIdWithPrefix(true)).isEqualTo("mopub:" + MOPUB_ID);
    }

    @Test
    public void getIdWithPrefix_whenUserConsentFalse_shouldReturnMopubString() {
        subject = new AdvertisingId(ANDROID_ID, MOPUB_ID, false, now);
        assertThat(subject.getIdWithPrefix(false)).isEqualTo("mopub:" + MOPUB_ID);
    }

    @Test
    public void getIdWithPrefix_whenUserConsentTrue_shouldReturnIfaString() {
        subject = new AdvertisingId(ANDROID_ID, MOPUB_ID, false, now);
        assertThat(subject.getIdWithPrefix(true)).isEqualTo("ifa:" + ANDROID_ID);
    }

    @Test
    public void getIdWithPrefix_whenLimitAdTrackingIsTrue_shouldNotDependOnConsent() {
        subject = new AdvertisingId(ANDROID_ID, MOPUB_ID, true, now);

        assertThat(subject.getIdWithPrefix(true)).isEqualTo("mopub:" + MOPUB_ID);
        assertThat(subject.getIdWithPrefix(false)).isEqualTo("mopub:" + MOPUB_ID);
    }

    @Test
    public void getIdentifier_whenDoNotTrackIsTrue_shouldReturnMoPubid() {
        subject = new AdvertisingId(ANDROID_ID, MOPUB_ID, true, now);

        assertThat(subject.getIdentifier(true)).isEqualTo(MOPUB_ID);
        assertThat(subject.getIdentifier(false)).isEqualTo(MOPUB_ID);
    }

    @Test
    public void getIdentifier_whenDoNotTrackIsFalse_shouldAnalyzeConsent() {
        subject = new AdvertisingId(ANDROID_ID, MOPUB_ID, false, now);
        
        assertThat(subject.getIdentifier(true)).isEqualTo(ANDROID_ID);
        assertThat(subject.getIdentifier(false)).isEqualTo(MOPUB_ID);
    }

    @Test
    public void generateExpiredAdvertisingId_shouldGenerateExpiredAdvertisingId() {
        subject = AdvertisingId.generateExpiredAdvertisingId();
        assertThat(subject.isRotationRequired()).isTrue();
    }

    @Test
    public void generateFreshAdvertisingId_shouldGenerateNonExpiredAdvertisingId() {
        subject = AdvertisingId.generateFreshAdvertisingId();
        assertThat(subject.isRotationRequired()).isFalse();
    }

    @Test
    public void generateIdString_lengthIs16x2plus4() {
        String uuid = AdvertisingId.generateIdString();
        assertThat(uuid.length()).isEqualTo(36);
    }
}
