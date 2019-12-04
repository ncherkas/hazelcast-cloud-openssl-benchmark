package com.hazelcast.cloud.benchmark.openssl;

import picocli.CommandLine;

public class RunCommand {

    @CommandLine.Option(names = {"-cu", "--cloudUrlBase"}, description = "Cloud URL base", defaultValue = "https://megatron.test.hazelcast.cloud")
    private String cloudUrlBase;
    @CommandLine.Parameters(paramLabel = "groupName", description = "Cluster group name", index = "0")
    private String groupName;
    @CommandLine.Parameters(paramLabel = "password", description = "Cluster group password", index = "1")
    private String groupPassword;
    @CommandLine.Parameters(paramLabel = "token", description = "Discovery token", index = "2")
    private String discoveryToken;
    @CommandLine.Option(names = {"-ssl", "--useSSL"}, description = "Whether to use SSL", type = Boolean.class, defaultValue = "false")
    private boolean useSSL;
    @CommandLine.Option(names = {"-st", "--sslType"}, description = "SSL type: java_tls or openssl")
    private String sslType;
    @CommandLine.Option(names = {"-scs", "--sslCipherSuites"}, description = "SSL ciphersuites")
    private String sslCipherSuites;
    @CommandLine.Option(names = {"-ks", "--sslKeyStore"}, description = "SSL Key Store")
    private String sslKeyStore;
    @CommandLine.Option(names = {"-ksp", "--sslKeyStorePassword"}, description = "SSL Key Store password")
    private String sslKeyStorePassword;
    @CommandLine.Option(names = {"-ts", "--sslTrustStore"}, description = "SSL Trust Store")
    private String sslTrustStore;
    @CommandLine.Option(names = {"-tsp", "--sslTrustStorePassword"}, description = "SSL Trust Store password")
    private String sslTrustStorePassword;
    @CommandLine.Parameters(paramLabel = "map", description = "Map name", index = "3")
    private String mapName;
    @CommandLine.Option(names = {"-cm", "--clearMapOnStart"}, description = "Whether to clear map on start", type = Boolean.class, defaultValue = "false")
    private boolean clearMapOnStart;
    @CommandLine.Option(names = {"-vs", "--valueSize"}, description = "Value byte array size", type = Integer.class, defaultValue = "8")
    private int valueSize;
    @CommandLine.Option(names = {"-kc", "--keysCount"}, description = "Generated keysCount count", type = Integer.class, defaultValue = "100")
    private int keysCount;
    @CommandLine.Option(names = {"-ic", "--iterationsCount"}, description = "Total iterations count", type = Integer.class, defaultValue = "1000")
    private long iterationsCount;
    @CommandLine.Option(names = {"-p", "--parallelism"}, description = "Parallelism", type = Integer.class, defaultValue = "5")
    private int parallelism;
    @CommandLine.Option(names = {"-wm", "--warmup"}, description = "Warm-up millis", type = Long.class, defaultValue = "-1")
    private long warmapMillis;
    @CommandLine.Option(names = {"-rl", "--rateLimit"}, description = "Rate limit per second", type = Double.class, defaultValue = "-1.0")
    private double rateLimit;
    @CommandLine.Option(names = {"-mt", "--maxTime"}, description = "Max time for test in millis", type = Long.class, defaultValue = "-1")
    private long maxTime;
    @CommandLine.Option(names={"-h", "--help"}, description="Display help/usage.", help=true)
    boolean help;

    public String getCloudUrlBase() {
        return cloudUrlBase;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getGroupPassword() {
        return groupPassword;
    }

    public String getDiscoveryToken() {
        return discoveryToken;
    }

    public boolean isUseSSL() {
        return useSSL;
    }

    public String getSslType() {
        return sslType;
    }

    public String getSslCipherSuites() {
        return sslCipherSuites;
    }

    public String getSslKeyStore() {
        return sslKeyStore;
    }

    public String getSslKeyStorePassword() {
        return sslKeyStorePassword;
    }

    public String getSslTrustStore() {
        return sslTrustStore;
    }

    public String getSslTrustStorePassword() {
        return sslTrustStorePassword;
    }

    public boolean isClearMapOnStart() {
        return clearMapOnStart;
    }

    public int getValueSize() {
        return valueSize;
    }

    public String getMapName() {
        return mapName;
    }

    public int getKeysCount() {
        return keysCount;
    }

    public long getIterationsCount() {
        return iterationsCount;
    }

    public int getParallelism() {
        return parallelism;
    }

    public long getWarmapMillis() {
        return warmapMillis;
    }

    public double getRateLimit() {
        return rateLimit;
    }

    public long getMaxTime() {
        return maxTime;
    }

    public boolean isHelp() {
        return help;
    }
}
