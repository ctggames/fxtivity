package io.github.ctgnz.fxtivity.consumer;

import java.time.LocalDate;

import io.github.ctgnz.fxtivity.Effective;
import io.github.ctgnz.fxtivity.EffectiveProperty;
import io.github.ctgnz.fxtivity.Effectivity;

/**
 * An owner a subclass in another package could extend, creating its property as {@link EffectiveProperty} documents.
 * <p>
 * javac's {@code this-escape} lint only examines classes like this one - public, not final, with a public constructor - and the build compiles with {@code -Xlint:all -Werror}. So
 * this compiling is the check that the documented suppression is the one an owner needs.
 */
public class ExtensibleOwner implements Effective {
    // EffectiveProperty only stores its owner while it is constructed, and reads its period later.
    @SuppressWarnings("this-escape")
    private final EffectiveProperty<String> name = new EffectiveProperty<>(this);

    /** An owner in effect from 1970, with no end. */
    public ExtensibleOwner() {
    }

    /**
     * The owner's name over time.
     *
     * @return the property
     */
    public EffectiveProperty<String> name() {
        return name;
    }

    @Override
    public LocalDate getStart() {
        return LocalDate.of(1970, 1, 1);
    }

    @Override
    public LocalDate getEnd() {
        return Effectivity.FOREVER;
    }

    @Override
    public void setStart(LocalDate startDate) {
    }

    @Override
    public void setEnd(LocalDate endDate) {
    }
}
