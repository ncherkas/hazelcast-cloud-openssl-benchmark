package com.hazelcast.cloud.benchmark.openssl;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;

import java.util.concurrent.TimeUnit;

/**
 * Used by default since it's more performant.
 */
public class DropwizardMetricsImpl implements IMetrics {

    private final MetricRegistry metricRegistry;
    private final ScheduledReporter reporter;

    public DropwizardMetricsImpl() {
        this.metricRegistry = new MetricRegistry();
        this.reporter = ConsoleReporter.forRegistry(metricRegistry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();

        reporter.start(10, TimeUnit.SECONDS);
    }

    @Override
    public void increment(String id) {
        metricRegistry.counter(id).inc();
    }

    @Override
    public void time(String id, long nanos) {
        metricRegistry.timer(id).update(nanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void happened(String id) {
        metricRegistry.meter(id).mark();
    }

    @Override
    public void print() {
        reporter.report();
    }

    @Override
    public void stop() {
        print();
        reporter.stop();
    }

    @Override
    public boolean isAutoReportingSupported() {
        return true;
    }
}
