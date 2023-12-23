package dev.micalobia.event;

public interface EventCondition<C> {
    boolean check(C context);
}
