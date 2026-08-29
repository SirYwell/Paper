package io.papermc.paper.event;

import org.bukkit.event.Event;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class EventGuard {
    private EventGuard() {}

    public static boolean hasListeners(Class<? extends Event> eventClass) {
        throw new UnsupportedOperationException("should be replaced");
    }
}
