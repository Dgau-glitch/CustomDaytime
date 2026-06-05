/*
 *     Copyright (c) 2026 Seedim
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package xyz.mayahive.customdaytime.common.event;

import xyz.mayahive.customdaytime.api.platform.PlatformScheduler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus {
    private final PlatformScheduler scheduler;
    private final Map<Class<?>, List<EventListener<?>>> listeners = new ConcurrentHashMap<>();

    public EventBus(PlatformScheduler scheduler) {
        this.scheduler = scheduler;
    }

    public <T extends Event>  void register(Class<T> eventType, EventListener<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public <T extends Event> void fire(T event) {
        scheduler.global().run(() -> dispatch(event));
    }

    @SuppressWarnings("unchecked")
    private <T extends Event> void dispatch(T event) {
        List<EventListener<?>> list = listeners.get(event.getClass());
        if (list == null) return;

        for (EventListener<?> listener : list) {
            ((EventListener<T>) listener).handle(event);
        }
    }
}
