package org.slamon;

import org.kie.api.event.process.*;
import com.timgroup.statsd.StatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import org.kie.api.runtime.KieRuntime;
import org.kie.api.runtime.process.ProcessInstance;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StatsdProcessEventListener implements ProcessEventListener {

    static final Logger log = Logger.getLogger(StatsdProcessEventListener.class.getCanonicalName());

    private final StatsDClient mClient;
    private StringBuilder mBuilder = new StringBuilder(32);
    private final Map<Long, Long> mRunningProcesses = new HashMap<>();
    private final ProcessCounterMap mTotalProcesses = new ProcessCounterMap();

    private class ProcessCounter {
        private long mValue;

        ProcessCounter(long initialValue) {
            mValue = initialValue;
        }

        long inc() {
            return ++mValue;
        }

        long dec() {
            return --mValue;
        }

        long get() {
            return mValue;
        }
    }

    private class ProcessCounterMap extends HashMap<String, ProcessCounter> {
        long inc(String processName) {
            ProcessCounter counter = get(processName);
            if (counter == null) {
                put(processName, new ProcessCounter(1));
                return 1;
            } else {
                return counter.inc();
            }
        }

        long inc(ProcessEvent event) {
            return inc(event.getProcessInstance().getProcessId());
        }

        long dec(String processName) {
            ProcessCounter counter = get(processName);
            if (counter == null) {
                put(processName, new ProcessCounter(-1));
                return -1;
            } else {
                return counter.dec();
            }
        }

        long dec(ProcessEvent event) {
            return dec(event.getProcessInstance().getProcessId());
        }
    }

    StatsdProcessEventListener(StatsDClient client) {
        mClient = client;
    }

    public StatsdProcessEventListener(String prefix, String host, int port) throws Exception {
        log.log(Level.INFO, "Loaded StatsD ProcessEventListener ({0},{1},{2})",
                new Object[]{prefix, host, Integer.toString(port)});
        if (prefix == null) {
            throw new java.lang.Exception("No StatsD prefix defined");
        } else if (host == null) {
            throw new java.lang.Exception("No StatsD host defined");
        } else if (port == 0) {
            throw new java.lang.Exception("Invalid StatsD port defined");
        }
        mClient = new NonBlockingStatsDClient(prefix, host, port);
    }

    public StatsdProcessEventListener(String host, int port) throws Exception {
        this(System.getProperty("slamon.statsd.prefix", "slamon.bpms"), host, port);
    }

    public StatsdProcessEventListener() throws Exception {
        this(System.getProperty("slamon.statsd.host"),
                Integer.parseInt(System.getProperty("slamon.statsd.port", "8125")));
    }

    static private String filterName(String name) {
        return name.replaceAll("[^a-zA-Z0-9]", "");
    }

    static private String createProcessName(ProcessInstance process, KieRuntime runtime) {
        long parentId = process.getParentProcessInstanceId();
        if (parentId != 0) {
            return createProcessName(runtime.getProcessInstance(parentId), runtime) + '.' + filterName(process.getProcessName());
        }
        return filterName(process.getProcessName());
    }

    private String createMetricName(ProcessEvent event, String suffix) {
        synchronized (mClient) {
            mBuilder.setLength(0);
            mBuilder.append("process.")
                    .append(createProcessName(event.getProcessInstance(), event.getKieRuntime()))
                    .append('.')
                    .append(suffix);
            return mBuilder.toString();
        }
    }

    @Override
    public void beforeProcessStarted(ProcessStartedEvent event) {

    }

    @Override
    public void afterProcessStarted(ProcessStartedEvent event) {
        log.log(Level.FINE, "Process {0} started at {1}", new Object[]{event.getProcessInstance().getId(), System.currentTimeMillis()});
        synchronized (mClient) {
            // Store process start time to calculate duration
            mRunningProcesses.put(event.getProcessInstance().getId(), System.currentTimeMillis());

            // send stats
            mClient.recordGaugeValue("process.running", mRunningProcesses.size());
            mClient.recordGaugeValue(createMetricName(event, "running"), mTotalProcesses.inc(event));
            mClient.incrementCounter(createMetricName(event, "started"));
        }
    }

    @Override
    public void beforeProcessCompleted(ProcessCompletedEvent event) {

    }

    @Override
    public void afterProcessCompleted(ProcessCompletedEvent event) {
        synchronized (mClient) {
            // calculate process duration from stored value
            long processStartTime = mRunningProcesses.remove(event.getProcessInstance().getId());

            // Generate metric name for recording stats
            String metricName;
            switch (event.getProcessInstance().getState()) {
                case ProcessInstance.STATE_COMPLETED:
                    metricName = createMetricName(event, "completed");
                    break;
                case ProcessInstance.STATE_ABORTED:
                    metricName = createMetricName(event, "aborted");
                    break;
                default:
                    log.log(Level.WARNING, "Process {0} completed with unexpected state: {1}", new Object[]{
                            event.getProcessInstance().getId(), event.getProcessInstance().getState()});
                    return;
            }

            log.log(Level.FINE, "Recording stats for process {0} with key {1}", new Object[]{
                    event.getProcessInstance().getId(), metricName});

            // send stats
            mClient.recordGaugeValue("process.running", mRunningProcesses.size());
            mClient.recordGaugeValue(createMetricName(event, "running"), mTotalProcesses.dec(event));
            mClient.recordExecutionTimeToNow(metricName, processStartTime);
        }
    }

    @Override
    public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {
    }

    @Override
    public void afterNodeTriggered(ProcessNodeTriggeredEvent event) {

    }

    @Override
    public void beforeNodeLeft(ProcessNodeLeftEvent event) {
    }

    @Override
    public void afterNodeLeft(ProcessNodeLeftEvent event) {
    }

    @Override
    public void beforeVariableChanged(ProcessVariableChangedEvent event) {
    }

    @Override
    public void afterVariableChanged(ProcessVariableChangedEvent event) {

    }
}