package io.github.ctgnz.fxtivity;

import java.util.List;
import java.util.SortedSet;

/**
 * What removing elements from a succession that allows no gaps does about the gap it would leave.
 * <p>
 * Removing the first or last element never leaves a gap, nor does any removal from a succession that allows gaps; this decides only the rest. It is chosen for each removal,
 * because the same succession can be edited both ways: whether a removed holder's time passes to a neighbour, and to which, or the removal is a mistake to report. A removal that
 * is not told - {@code remove(int)}, {@code clear()}, an iterator - is {@link #Refused}.
 *
 * @see SingleEffectiveList#remove(int, Removal)
 */
public enum Removal {

        /** The removal is refused with {@link IllegalArgumentException}, and nothing changes. */
        Refused {
            @Override
            <E extends Effective> void remove(SingleEffectiveList<E> list, SortedSet<Integer> indices, List<SingleEffectiveList.Gap<E>> gaps) {
                if (!gaps.isEmpty()) {
                    SingleEffectiveList.Gap<E> gap = gaps.getFirst();
                    throw new IllegalArgumentException("Removing would leave nothing in effect between " + gap.previous() + " and " + gap.next());
                }
                list.removeAt(indices);
            }
        },

        /** The element before the gap is extended forwards to where the element after it starts. */
        ExtendsPrevious {
            @Override
            <E extends Effective> void remove(SingleEffectiveList<E> list, SortedSet<Integer> indices, List<SingleEffectiveList.Gap<E>> gaps) {
                list.removeAt(indices);
                gaps.forEach(gap -> list.move(list.indexOf(gap.previous()), gap.previous().getStart(), gap.next().getStart()));
            }
        },

        /** The element after the gap is started earlier, where the element before it ends. */
        StartsNextEarlier {
            @Override
            <E extends Effective> void remove(SingleEffectiveList<E> list, SortedSet<Integer> indices, List<SingleEffectiveList.Gap<E>> gaps) {
                list.removeAt(indices);
                gaps.forEach(gap -> list.move(list.indexOf(gap.next()), gap.previous().getEnd(), gap.next().getEnd()));
            }
        };

    // Removes the elements at indices from list, given the gaps their removal would leave - each between two elements that remain - and does what this removal does about them.
    abstract <E extends Effective> void remove(SingleEffectiveList<E> list, SortedSet<Integer> indices, List<SingleEffectiveList.Gap<E>> gaps);

}
