package com.hazelcast.cloud.benchmark.openssl;

public interface IMetrics {

    void increment(String id);

    void time(String id, long nanos);

    void happened(String id);

    void print();

    void stop();

    boolean isAutoReportingSupported();
}
