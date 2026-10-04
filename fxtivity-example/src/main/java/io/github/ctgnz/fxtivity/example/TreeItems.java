package io.github.ctgnz.fxtivity.example;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

/**
 * Keeps a tree item's children in step with an observable list - typically an effective list's {@code effective()} view, so the tree follows the effective date.
 * <p>
 * A {@link TreeItem}'s children are a plain list of its own: filling them once from an effective list gives the tree the state on that day and no other. They have to follow the
 * list instead, which is what this does. Each element keeps the same tree item for as long as it is in the list, so a branch that leaves and comes back - a department open again
 * on a later date - keeps its own children and whether it is expanded.
 */
final class TreeItems {

    private TreeItems() {
    }

    /**
     * Makes {@code parent}'s children follow {@code source}, one tree item per element, made by {@code factory}.
     *
     * @param <S>
     *            the element type
     * @param parent
     *            the tree item whose children follow the list
     * @param source
     *            the list to follow
     * @param factory
     *            makes the tree item for an element, the first time it is in the list
     */
    static <S> void bindChildren(TreeItem<Object> parent, ObservableList<S> source, Function<S, TreeItem<Object>> factory) {
        // Every element that has been in the list keeps its item - at most one per element of the model, which the tree is a view of.
        Map<S, TreeItem<Object>> items = new IdentityHashMap<>();
        Runnable follow = () -> {
            List<TreeItem<Object>> children = source.stream().map(element -> items.computeIfAbsent(element, factory)).toList();
            parent.getChildren().setAll(children);
        };
        follow.run();
        // A strong listener: the source belongs to the model the tree shows, and the two are discarded together.
        source.addListener((ListChangeListener<S>) change -> follow.run());
    }

}
