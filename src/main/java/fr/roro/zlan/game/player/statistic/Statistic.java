package fr.roro.zlan.game.player.statistic;

/**
 * This file is a part of ZLAN project.
 *
 * @author roro1506_HD
 */
public abstract class Statistic<T> {

    T value;

    Statistic(T defaultValue) {
        this.value = defaultValue;
    }

    public T getValue() {
        return this.value;
    }

    public void setValue(T value) {
        this.value = value;
    }
}
