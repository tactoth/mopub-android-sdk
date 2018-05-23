package com.mopub.common.util;

import java.util.concurrent.TimeUnit;

/**
 * Expiration
 * Created by liuwei on 5/23/18.
 */
public class Expiration {
  private final long durationNs;
  private long ts = 0;

  public Expiration(long duration, TimeUnit unit) {
    this.durationNs = unit.toNanos(duration);
  }

  public synchronized boolean isExpired() {
    return ts == 0 || ts + durationNs < System.nanoTime();
  }

  public synchronized void refresh() {
    ts = System.nanoTime();
  }
}
