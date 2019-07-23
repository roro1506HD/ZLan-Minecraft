package fr.roro.zlan.game.player.statistic;

/**
 * This file is a part of ZLAN project.
 *
 * @author roro1506_HD
 */
public class StatisticInteger extends Statistic<Integer> {

    StatisticInteger(int defaultValue) {
        super(defaultValue);
    }

    public void incrementValue(int amount) {
        this.value += amount;
    }

    public void decrementValue(int amount) {
        this.value -= amount;
    }
}
