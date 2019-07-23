package fr.roro.zlan.game.player.statistic;

import java.util.function.Function;

/**
 * This file is a part of ZLAN project.
 *
 * @author roro1506_HD
 */
public class StatisticType<T, R extends Statistic<T>> {

    //@formatter:off
    //public static final StatisticType<Integer, StatisticInteger> KILLS       = new StatisticType<>(0, StatisticInteger::new);
    public static final StatisticType<Integer, StatisticInteger> TOTAL_KILLS = new StatisticType<>(0, StatisticInteger::new);
    //@formatter:on

    private final T              defaultValue;
    private final Function<T, R> instantiator;

    private StatisticType(T defaultValue, Function<T, R> instantiator) {
        this.defaultValue = defaultValue;
        this.instantiator = instantiator;
    }

    public T getDefaultValue() {
        return this.defaultValue;
    }

    public R getDefaultStatistic() {
        return this.instantiator.apply(this.defaultValue);
    }
}
