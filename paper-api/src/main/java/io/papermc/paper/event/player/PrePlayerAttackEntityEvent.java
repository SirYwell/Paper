package io.papermc.paper.event.player;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

/**
 * Called when the player tries to attack an entity.
 * <p>
 * This occurs before any of the damage logic, so cancelling this event
 * will prevent any sort of sounds from being played when attacking.
 * <p>
 * This event will fire as cancelled for certain entities, with {@link PrePlayerAttackEntityEvent#willAttack()} being false
 * to indicate that this entity will not actually be attacked.
 * <p>
 * Note: there may be other factors (invulnerability, etc.) that will prevent this entity from being attacked that this event will not cover
 */
@NullMarked
public class PrePlayerAttackEntityEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Entity attacked;
    private final boolean willAttack;

    private boolean cancelled;

    @ApiStatus.Internal
    public PrePlayerAttackEntityEvent(final Player player, final Entity attacked, final boolean willAttack) {
        super(player);
        this.attacked = attacked;
        this.willAttack = willAttack;
        this.cancelled = !willAttack;
    }

    /**
     * Gets the entity that was attacked in this event.
     *
     * @return entity that was attacked
     */
    public Entity getAttacked() {
        return this.attacked;
    }

    /**
     * Gets if this entity will be attacked normally.
     * Entities like falling sand will return {@code false} because
     * their entity type does not allow them to be attacked.
     * <p>
     * Note: there may be other factors (invulnerability, etc.) that will prevent this entity from being attacked that this event will not cover
     *
     * @return if the entity will actually be attacked
     */
    public boolean willAttack() {
        return this.willAttack;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    /**
     * Sets if this attack should be cancelled, note if {@link PrePlayerAttackEntityEvent#willAttack()} returns false
     * this event will always be cancelled.
     *
     * @param cancel {@code true} if you wish to cancel this event
     */
    @Override
    public void setCancelled(final boolean cancel) {
        if (!this.willAttack) {
            return;
        }

        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
