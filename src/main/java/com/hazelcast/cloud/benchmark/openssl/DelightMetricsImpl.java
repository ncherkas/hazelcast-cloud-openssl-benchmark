package com.hazelcast.cloud.benchmark.openssl;

import de.mxro.metrics.MetricsCommon;
import de.mxro.metrics.jre.Metrics;
import delight.async.properties.PropertyNode;

import java.util.concurrent.TimeUnit;

/**
 * This impl is slower when used by multiple threads.
 */
public class DelightMetricsImpl implements IMetrics {

    private final PropertyNode metrics = Metrics.create();;

    @Override
    public void increment(String id) {
        metrics.record(MetricsCommon.increment(id));
    }

    @Override
    public void time(String id, long nanos) {
        metrics.record(MetricsCommon.value(id, TimeUnit.NANOSECONDS.toMillis(nanos)));
    }

    @Override
    public void happened(String id) {
        metrics.record(MetricsCommon.happened(id));
    }

    @Override
    public void print() {
        metrics.print();
    }

    @Override
    public void stop() {
        print();
        metrics.stop().get();
    }

    @Override
    public boolean isAutoReportingSupported() {
        return false;
    }
}
