package me.alek.honey.netty.api.event;

import me.alek.honey.netty.api.event.impl.packet.*;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.api.event.impl.inject.InjectEvent;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventManager {

    private final Map<Class<? extends InjectEvent>, List<EventExecutor>> injectEvents = new HashMap<>();
    private final Map<Class<? extends PacketEvent>, List<EventExecutor>> packetEvents = new HashMap<>();
    private final Map<PacketState, Map<PacketBound, Class<? extends PacketEvent>>> PACKET_EVENT_CLASS_CACHE;

    public EventManager() {
        PACKET_EVENT_CLASS_CACHE = new HashMap<PacketState, Map<PacketBound, Class<? extends PacketEvent>>>(){{
            put(PacketState.PLAY, new HashMap<PacketBound, Class<? extends PacketEvent>>(){{
                put(PacketBound.SERVER, PacketPlaySendEvent.class);
                put(PacketBound.CLIENT, PacketPlayReceiveEvent.class);
            }});
            put(PacketState.HANDSHAKE, new HashMap<PacketBound, Class<? extends PacketEvent>>(){{
                put(PacketBound.CLIENT, PacketHandshakeReceiveEvent.class);
            }});
            put(PacketState.LOGIN, new HashMap<PacketBound, Class<? extends PacketEvent>>(){{
                put(PacketBound.SERVER, PacketLoginSendEvent.class);
                put(PacketBound.CLIENT, PacketLoginReceiveEvent.class);
            }});
            put(PacketState.STATUS, new HashMap<PacketBound, Class<? extends PacketEvent>>(){{
                put(PacketBound.SERVER, PacketStatusSendEvent.class);
                put(PacketBound.CLIENT, PacketStatusReceiveEvent.class);
            }});
        }};
    }

    public void addListener(Object object) {
        for (Method method : object.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Listener.class) ) {

                if (Event.class.isAssignableFrom(method.getParameterTypes()[0])) {
                    Class<? extends Event> clazz = method.getParameterTypes()[0].asSubclass(Event.class);

                    List<EventExecutor> eventExecutors;
                    if (PacketEvent.class.isAssignableFrom(clazz)) {
                        eventExecutors = packetEvents.get(clazz);

                        if (eventExecutors == null) {
                            packetEvents.put((Class<? extends PacketEvent>) clazz, new ArrayList<>());
                            eventExecutors = packetEvents.get(clazz);
                        }
                    }
                    else if (InjectEvent.class.isAssignableFrom(clazz)) {
                        eventExecutors = injectEvents.get(clazz);

                        if (eventExecutors == null) {
                            injectEvents.put((Class<? extends InjectEvent>) clazz, new ArrayList<>());
                            eventExecutors = injectEvents.get(clazz);
                        }
                    }
                    else {
                        continue;
                    }

                    eventExecutors.add((event) -> {
                        try {
                            method.invoke(object, event);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
                }
            }
        }
    }

    public void callListeners(Event event, boolean isPacket) {
        List<EventExecutor> executors = (isPacket) ? packetEvents.get(event.getClass()) : injectEvents.get(event.getClass());
        if (executors == null) {
            return;
        }
        for (EventExecutor executor : executors) {
            executor.call(event);
        }
    }

    public boolean hasHandlers(Class<? extends Event> clazz, boolean isPacket) {
        if (isPacket) {
            return packetEvents.containsKey(clazz);
        }
        else {
            return injectEvents.containsKey(clazz);
        }
    }

    public Class<? extends PacketEvent> getPacketEventFor(PacketState state, PacketBound bound) {
        return PACKET_EVENT_CLASS_CACHE.get(state).get(bound);
    }
}
