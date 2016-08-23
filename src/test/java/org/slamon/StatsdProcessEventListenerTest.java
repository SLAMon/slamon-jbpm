package org.slamon;

import com.timgroup.statsd.StatsDClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.kie.api.event.process.ProcessCompletedEvent;
import org.kie.api.event.process.ProcessEvent;
import org.kie.api.event.process.ProcessStartedEvent;
import org.kie.api.runtime.process.ProcessInstance;


import static org.mockito.Mockito.*;


public class StatsdProcessEventListenerTest {

    @Mock
    private StatsDClient mMockClient;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    ProcessInstance mockProcess(final String name) {
        final ProcessInstance mockInstance = mock(ProcessInstance.class);
        when(mockInstance.getId()).thenReturn((long) mockInstance.hashCode());
        when(mockInstance.getProcessId()).thenReturn(name);
        when(mockInstance.getProcessName()).thenReturn(name);
        return mockInstance;
    }

    static <T extends ProcessEvent> T mockProcessEvent(Class<T> eventClass, ProcessInstance process) {
        T mockEvent = mock(eventClass);
        when(mockEvent.getProcessInstance()).thenReturn(process);
        return mockEvent;
    }

    @Test
    public void testRunningGauges() throws Exception {
        StatsdProcessEventListener listener = new StatsdProcessEventListener(mMockClient);

        ProcessInstance mockInstance1 = mockProcess("Test Process-2");
        ProcessInstance mockInstance2 = mockProcess("Test Process-2");
        ProcessInstance mockInstance3 = mockProcess("Another Test Process-1");

        // start three processes
        listener.afterProcessStarted(mockProcessEvent(ProcessStartedEvent.class, mockInstance1));
        listener.afterProcessStarted(mockProcessEvent(ProcessStartedEvent.class, mockInstance2));
        listener.afterProcessStarted(mockProcessEvent(ProcessStartedEvent.class, mockInstance3));

        // complete two processes, one with completed status and one with aborted status
        when(mockInstance2.getState()).thenReturn(ProcessInstance.STATE_ABORTED);
        listener.afterProcessCompleted(mockProcessEvent(ProcessCompletedEvent.class, mockInstance2));
        when(mockInstance1.getState()).thenReturn(ProcessInstance.STATE_COMPLETED);
        listener.afterProcessCompleted(mockProcessEvent(ProcessCompletedEvent.class, mockInstance1));

        // verify total running gauge
        verify(mMockClient, times(2)).recordGaugeValue("process.running", 1);
        verify(mMockClient, times(2)).recordGaugeValue("process.running", 2);
        verify(mMockClient, times(1)).recordGaugeValue("process.running", 3);

        // verify per process definition gauges
        verify(mMockClient, times(2)).recordGaugeValue("process.TestProcess2.running", 1);
        verify(mMockClient).recordGaugeValue("process.TestProcess2.running", 2);
        verify(mMockClient).recordGaugeValue("process.TestProcess2.running", 0);
        verify(mMockClient).recordGaugeValue("process.AnotherTestProcess1.running", 1);
    }

    @Test
    public void testRunningTimeEmitted() throws Exception {
        StatsdProcessEventListener listener = new StatsdProcessEventListener(mMockClient);

        ProcessInstance mockInstance1 = mockProcess("Test Process-2");
        listener.afterProcessStarted(mockProcessEvent(ProcessStartedEvent.class, mockInstance1));

        when(mockInstance1.getState()).thenReturn(ProcessInstance.STATE_COMPLETED);
        listener.afterProcessCompleted(mockProcessEvent(ProcessCompletedEvent.class, mockInstance1));

        verify(mMockClient).recordExecutionTimeToNow(eq("process.TestProcess2.completed"), anyLong());
    }

    @Test
    public void testCounters() throws Exception {
        StatsdProcessEventListener listener = new StatsdProcessEventListener(mMockClient);

        ProcessInstance mockInstance1 = mockProcess("Test Process-2");
        ProcessInstance mockInstance2 = mockProcess("Test Process-2");
        ProcessInstance mockInstance3 = mockProcess("Another Test Process-1");

        // start three processes
        listener.afterProcessStarted(mockProcessEvent(ProcessStartedEvent.class, mockInstance1));
        listener.afterProcessStarted(mockProcessEvent(ProcessStartedEvent.class, mockInstance2));
        listener.afterProcessStarted(mockProcessEvent(ProcessStartedEvent.class, mockInstance3));

        // complete two processes, one with completed status and one with aborted status
        when(mockInstance2.getState()).thenReturn(ProcessInstance.STATE_ABORTED);
        listener.afterProcessCompleted(mockProcessEvent(ProcessCompletedEvent.class, mockInstance2));
        when(mockInstance1.getState()).thenReturn(ProcessInstance.STATE_COMPLETED);
        listener.afterProcessCompleted(mockProcessEvent(ProcessCompletedEvent.class, mockInstance1));

        verify(mMockClient, times(2)).incrementCounter("process.TestProcess2.started");
        verify(mMockClient, times(1)).incrementCounter("process.AnotherTestProcess1.started");
    }

    /** With persistence enabled, a task previously unknown to event listener may complete. */
    @Test
    public void testUnknownEventCompletion() throws Exception {
        StatsdProcessEventListener listener = new StatsdProcessEventListener(mMockClient);

        ProcessInstance mockInstance1 = mockProcess("Test Process-1");
        when(mockInstance1.getState()).thenReturn(ProcessInstance.STATE_COMPLETED);
        // complete event
        listener.afterProcessCompleted(mockProcessEvent(ProcessCompletedEvent.class, mockInstance1));


        verify(mMockClient, never()).recordExecutionTimeToNow(anyString(), anyLong());
    }

    @Test
    public void testUnknownState() throws Exception {
        StatsdProcessEventListener listener = new StatsdProcessEventListener(mMockClient);

        ProcessInstance mockInstance1 = mockProcess("Test Process-2");
        listener.afterProcessStarted(mockProcessEvent(ProcessStartedEvent.class, mockInstance1));

        // reset counters after start
        reset(mMockClient);

        when(mockInstance1.getState()).thenReturn(1337);
        listener.afterProcessCompleted(mockProcessEvent(ProcessCompletedEvent.class, mockInstance1));

        verify(mMockClient, never()).recordExecutionTimeToNow(anyString(), anyLong());
        verify(mMockClient, never()).recordGaugeValue(anyString(), anyLong());
    }

    @Test
    public void testAfterProcessCompleted() throws Exception {

    }
}