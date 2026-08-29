package io.papermc.paper.event;

import com.destroystokyo.paper.util.SneakyThrow;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

public final class EventBootstrap {
    private static final MethodHandle RELINK_TO_GENERIC;
    private static final MethodHandle RECEIVER_HAS_EXACT_CLASS;
    private static final MethodType CALL_EVENT_TYPE = MethodType.methodType(boolean.class);

    static {
        try {
            RELINK_TO_GENERIC = MethodHandles.lookup().findStatic(
                EventBootstrap.class,
                "relinkToGeneric",
                MethodType.methodType(boolean.class, MutableCallSite.class, MethodHandle.class, Event.class)
            );
            RECEIVER_HAS_EXACT_CLASS = MethodHandles.lookup().findStatic(
                EventBootstrap.class,
                "receiverHasExactClass",
                MethodType.methodType(boolean.class, Event.class, Class.class)
            );
        } catch (final NoSuchMethodException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    private EventBootstrap() {
    }

    public static CallSite createForCallEvent(
        final MethodHandles.Lookup lookup,
        final String invocationName,
        final MethodType invocationType,
        final Class<?> staticEventClass
    ) {
        try {
            final Class<? extends Event> typedEventClass = staticEventClass.asSubclass(Event.class);
            final MutableCallSite callSite = new MutableCallSite(invocationType);
            final MethodHandle genericTarget = lookup.findVirtual(typedEventClass, invocationName, CALL_EVENT_TYPE).asType(invocationType);
            final MethodHandle specializedTarget = EventBootstrap.targetForStaticType(lookup, typedEventClass, invocationName, invocationType);
            final MethodHandle receiverCheck = MethodHandles.insertArguments(RECEIVER_HAS_EXACT_CLASS, 1, staticEventClass)
                .asType(MethodType.methodType(boolean.class, invocationType.parameterType(0)));
            final MethodHandle fallbackTarget = MethodHandles.insertArguments(RELINK_TO_GENERIC, 0, callSite, genericTarget)
                .asType(invocationType);

            callSite.setTarget(MethodHandles.guardWithTest(receiverCheck, specializedTarget, fallbackTarget));
            return callSite;
        } catch (final ReflectiveOperationException ex) {
            throw new RuntimeException("Failed to resolve callEvent target for " + staticEventClass.getName(), ex);
        }
    }

    public static boolean hasListenersFast(MutableCallSite callSite, Class<?> actual, Class<?> expected, MethodHandle call) {
        if (actual != expected) {
            // TODO fallback
        }
        try {
            return (boolean) call.invokeExact();
        } catch (Throwable e) {
            SneakyThrow.sneaky(e);
            return false; // unreachable
        }
    }

    private static boolean receiverHasExactClass(final Event event, final Class<?> staticEventClass) {
        return event.getClass() == staticEventClass;
    }

    private static boolean relinkToGeneric(final MutableCallSite callSite, final MethodHandle genericTarget, final Event event) throws Throwable {
        System.out.println("relinking due to " + event + " " + genericTarget + " " + callSite.toString());
        new Exception().printStackTrace();
        callSite.setTarget(genericTarget.asType(callSite.type()));
        MutableCallSite.syncAll(new MutableCallSite[] {callSite});
        // the genericTarget expects the static event type of the call site, so invokeExact does not work
        return (boolean) genericTarget.invoke(event);
    }

    private static MethodHandle targetForStaticType(
        final MethodHandles.Lookup lookup,
        final Class<? extends Event> staticEventClass,
        final String invocationName,
        final MethodType invocationType
    ) throws ReflectiveOperationException {
        if (staticEventClass.getMethod(invocationName).getDeclaringClass() != Event.class) {
            System.out.println("static type with override " + staticEventClass);
            new Exception().printStackTrace();
            return lookup.findVirtual(staticEventClass, invocationName, CALL_EVENT_TYPE).asType(invocationType);
        }

        if (staticEventClass == Event.class) {
            System.out.println("static type is event");
            new Exception().printStackTrace();
            return lookup.findVirtual(Event.class, invocationName, CALL_EVENT_TYPE).asType(invocationType);
        }

        return EventBootstrap.findHandlerList(staticEventClass).callEvent.dynamicInvoker().asType(invocationType);
    }

    private static HandlerList findHandlerList(final Class<? extends Event> eventClass) throws ReflectiveOperationException {
        for (Class<?> current = eventClass; Event.class.isAssignableFrom(current); current = current.getSuperclass()) {
            try {
                return (HandlerList) current.getDeclaredMethod("getHandlerList").invoke(null);
            } catch (final NoSuchMethodException ignored) {
                // continue walking up the event hierarchy
            }
        }
        throw new NoSuchMethodException("No static getHandlerList() found for " + eventClass.getName());
    }
}
