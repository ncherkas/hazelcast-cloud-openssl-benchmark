package com.hazelcast.cloud.benchmark.openssl;

import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Bytes;
import com.google.common.util.concurrent.RateLimiter;
import com.google.common.util.concurrent.Uninterruptibles;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.spi.impl.discovery.HazelcastCloudDiscovery;
import com.hazelcast.client.spi.properties.ClientProperty;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.SSLConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import picocli.CommandLine;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;

/**
 * Benchmark tool runner. See {@code RunCommand} for options.
 */
public class TestRunner {

    private static final int METRICS_PERIOD = 10_000;

    private final IMap<Integer, byte[]> map;
    private final boolean isClearMapOnStart;
    private final int valueSize;
    private final long iterationsCount;
    private final int parallelism;
    private final long warmapMillis;
    private final long maxTime;
    private final int keysCount;
    private final RateLimiter rateLimiter;

    public TestRunner(RunCommand command) {
        HazelcastInstance client = createClient(command);
        this.map = client.getMap(command.getMapName());
        System.out.println("Current map size: " + map.size());

        this.isClearMapOnStart = command.isClearMapOnStart();
        this.valueSize = command.getValueSize();
        this.iterationsCount = command.getIterationsCount();
        this.parallelism = command.getParallelism();
        this.warmapMillis = command.getWarmapMillis();
        this.maxTime = command.getMaxTime();
        this.keysCount = command.getKeysCount();
        this.rateLimiter = command.getRateLimit() > 0 ? RateLimiter.create(command.getRateLimit()) : null;
    }

    private HazelcastInstance createClient(RunCommand command) {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setGroupConfig(new GroupConfig(command.getGroupName(), command.getGroupPassword()));
        clientConfig.setProperty("hazelcast.client.statistics.enabled","true");
        clientConfig.setProperty(ClientProperty.HAZELCAST_CLOUD_DISCOVERY_TOKEN.getName(), command.getDiscoveryToken());
        clientConfig.setProperty(HazelcastCloudDiscovery.CLOUD_URL_BASE_PROPERTY.getName(), command.getCloudUrlBase());

        if (command.isUseSSL()) {
            System.out.println("Enabling Hazelcast Client SSL config '" + command.getSslType() + "'");
            SSLConfig sslConfig;
            if (command.getSslType().equals("java_tls")) {
                Properties props = new Properties();
                props.setProperty("keyStore", command.getSslKeyStore());
                props.setProperty("keyStorePassword", command.getSslKeyStorePassword());
                props.setProperty("trustStore", command.getSslTrustStore());
                props.setProperty("trustStorePassword", command.getSslTrustStorePassword());
                props.setProperty("protocol", "TLSv1.2");
                if (!Strings.isNullOrEmpty(command.getSslCipherSuites())) {
                    props.setProperty("ciphersuites", command.getSslCipherSuites());
                }
                sslConfig = new SSLConfig().setEnabled(true)
                        .setFactoryClassName("com.hazelcast.nio.ssl.BasicSSLContextFactory")
                        .setProperties(props);
            } else if (command.getSslType().equals("openssl")) {
                Properties props = new Properties();
                props.setProperty("keyStore", command.getSslKeyStore());
                props.setProperty("keyStorePassword", command.getSslKeyStorePassword());
                props.setProperty("trustStore", command.getSslTrustStore());
                props.setProperty("trustStorePassword", command.getSslTrustStorePassword());
                props.setProperty("keyManagerAlgorithm", "SunX509");
                props.setProperty("trustManagerAlgorithm", "SunX509");
                props.setProperty("protocol", "TLSv1.2");
                if (!Strings.isNullOrEmpty(command.getSslCipherSuites())) {
                    props.setProperty("ciphersuites", command.getSslCipherSuites());
                }
                sslConfig = new SSLConfig().setEnabled(true)
                        .setFactoryClassName("com.hazelcast.nio.ssl.OpenSSLEngineFactory")
                        .setProperties(props);
            } else {
                throw new UnsupportedOperationException();
            }

            clientConfig.getNetworkConfig().setSSLConfig(sslConfig);
        }

        return HazelcastClient.newHazelcastClient(clientConfig);
    }

    public static void main(String[] args) throws Exception {
        RunCommand command = CommandLine.populateCommand(new RunCommand(), args);
        if (command.isHelp()) {
            CommandLine.usage(command, System.out, CommandLine.Help.Ansi.AUTO);
            return;
        }

        LocalDateTime startTime = LocalDateTime.now();
        System.out.println(startTime + ":: Starting with the following parameters: ");
        System.out.println("\tgroupName='" + command.getGroupName() + '\'' +
                ", groupPassword='******'" +
                ", cloudUrlBase='" + command.getCloudUrlBase() + '\'' +
                ", discoveryToken='" + command.getDiscoveryToken() + '\'' +
                ", useSSL=" + command.isUseSSL() +
                ", sslType='" + command.getSslType() + '\'' +
                ", sslCipherSuites='" + command.getSslCipherSuites() + '\'' +
                ", clearMapOnStart=" + command.isClearMapOnStart() +
                ", mapName='" + command.getMapName() + '\'' +
                ", keysCount=" + command.getKeysCount() +
                ", valueSize=" + command.getValueSize() +
                ", iterationsCount=" + command.getIterationsCount() +
                ", parallelism=" + command.getParallelism() +
                ", warmapMillis=" + command.getWarmapMillis() +
                ", rateLimit=" + command.getRateLimit() +
                ", maxTime=" + command.getMaxTime() + '\'');

        TestRunner testRunner = new TestRunner(command);

        testRunner.run();

        System.out.println(LocalDateTime.now() + ":: Finished! (was started at " + startTime + ")");
    }

    private void run() throws InterruptedException {
        if (isClearMapOnStart) {
            clearMapOnStart();
        }

        IMetrics metrics = new DropwizardMetricsImpl();
        ExecutorService scheduledMetricsExecutor = null;
        if (!metrics.isAutoReportingSupported()) {
            scheduledMetricsExecutor = scheduleMetricsOutput(metrics);
        }

        ExecutorService testThreadsExecutor = Executors.newCachedThreadPool();
        Runnable testRunnable = () -> {
            Stopwatch stopwatch = Stopwatch.createStarted();

            List<Integer> keys = IntStream.range(0, keysCount).boxed().collect(toList());
            ThreadLocalRandom localRandom = ThreadLocalRandom.current();
            Collections.shuffle(keys, localRandom);

            Iterator<Integer> keysIterator = Iterables.cycle(keys).iterator();

            for (int i = 0; i < iterationsCount / parallelism; i++) {
                if (rateLimiter != null) {
                    rateLimiter.acquire();
                }

                // Keys are looped in a repeating cycle
                Integer key = keysIterator.next();

                // 1st we read an existing time
                invokeRead(key, metrics);

                // 2nd we update a time
                invokeWrite(key, metrics);

                if (maxTime > 0 && stopwatch.elapsed(TimeUnit.MILLISECONDS) >= maxTime) {
                    System.out.println("Stopping " + Thread.currentThread());
                    break;
                }
            }
        };

        CompletableFuture[] completableFutures = new CompletableFuture[parallelism];
        int warmUpPauses = parallelism - 1;
        for (int i = 0; i < parallelism; i++) {
            completableFutures[i] = CompletableFuture.runAsync(testRunnable, testThreadsExecutor);
            if (warmapMillis > 0 && i < warmUpPauses) {
                MILLISECONDS.sleep(warmapMillis / warmUpPauses);
            }

            System.out.println("Running parallel thread #" + i);
        }

        CompletableFuture.allOf(completableFutures).join();

        testThreadsExecutor.shutdown();
        if (scheduledMetricsExecutor != null) {
            scheduledMetricsExecutor.shutdownNow();
        }

        TimeUnit.SECONDS.sleep(1);
        metrics.stop();
        HazelcastClient.shutdownAll();
    }

    private void clearMapOnStart() {
        System.out.println(LocalDateTime.now() + ":: Clearing map data (can take some time)");
        map.clear();
        System.out.println(LocalDateTime.now() + ":: Map data cleared");
        Uninterruptibles.sleepUninterruptibly(10, TimeUnit.SECONDS);
    }

    private ExecutorService scheduleMetricsOutput(IMetrics metrics) {
        ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        long initialDelay = warmapMillis > 0 ? warmapMillis : METRICS_PERIOD;
        scheduledExecutor.scheduleAtFixedRate(() -> {
            System.out.println("At " + LocalDateTime.now() + "::");
            metrics.print();
        }, initialDelay, METRICS_PERIOD, MILLISECONDS); // TODO: configure
        return scheduledExecutor;
    }

    private void invokeRead(Integer key, IMetrics metrics) {
        long readStartNanos = System.nanoTime();
        map.get(key);
        long readTimeNanos = System.nanoTime() - readStartNanos;

        String readMetricId = "read_byte[" + valueSize + "]";
        metrics.time(readMetricId + "_latency", readTimeNanos);
        metrics.happened(readMetricId);
        metrics.increment("total_operations");
    }

    private void invokeWrite(Integer key, IMetrics metrics) {
        String value = key + "@" + System.nanoTime();
        byte[] payload = Bytes.ensureCapacity(value.getBytes(), valueSize, 0);

        long writeStartNanos = System.nanoTime();
        map.put(key, payload);
        long writeTimeNanos = System.nanoTime() - writeStartNanos;

        String writeMetricId = "write_byte[" + valueSize + "]";
        metrics.time(writeMetricId + "_latency", writeTimeNanos);
        metrics.happened(writeMetricId);
        metrics.increment("total_operations");
    }
}
