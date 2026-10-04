package io.github.ctgnz.fxtivity.cucumber;

import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.PickleStepTestStep;
import io.cucumber.plugin.event.TestCaseStarted;
import io.cucumber.plugin.event.TestStepStarted;

/**
 * Tells the steps when a scenario reaches its first {@code When} step, so they can draw the scenario as it stood before the action - the picture to compare the result with.
 * <p>
 * A plugin, because only the events say which keyword a step was written with. A concurrent listener, because a plain one is told of events only after the run, and the picture has
 * to be taken now; it is told on the thread running the scenario, which is where the steps leave what to do.
 */
public final class BeforeTheAction implements ConcurrentEventListener {

    private static final ThreadLocal<Runnable> ACTION = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> SEEN = ThreadLocal.withInitial(() -> false);

    /** What to do just before the scenario running on this thread reaches its first {@code When} step. */
    static void onFirstAction(Runnable action) {
        ACTION.set(action);
    }

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestCaseStarted.class, event -> SEEN.set(false));
        publisher.registerHandlerFor(TestStepStarted.class, event -> {
            if (event.getTestStep() instanceof PickleStepTestStep step && step.getStep().getKeyword().strip().equals("When") && !SEEN.get()) {
                SEEN.set(true);
                Runnable action = ACTION.get();
                if (action != null) {
                    action.run();
                }
            }
        });
    }

}
