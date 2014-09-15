/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.events.publishers;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ambari.server.events.AlertEvent;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Singleton;

/**
 * The {@link AlertEventPublisher} is used to wrap a customized instance of an
 * {@link AsyncEventBus} that is only used for alerts. In general, Ambari should
 * have its own application-wide event bus for application events (session
 * information, state changes, etc), but since alerts can contain many events
 * being published concurrently, it makes sense to encapsulate a specific alert
 * bus in this publisher.
 */
@Singleton
public final class AlertEventPublisher {

  /**
   * A multi-threaded event bus that can handle dispatching {@link AlertEvent}s.
   */
  private final EventBus s_eventBus;

  /**
   * Constructor.
   */
  public AlertEventPublisher() {
    s_eventBus = new AsyncEventBus(Executors.newFixedThreadPool(2,
        new AlertEventBusThreadFactory()));
  }

  /**
   * Publishes the specified event to all registered listeners that
   * {@link Subscribe} to any of the {@link AlertEvent} instances.
   *
   * @param event
   */
  public void publish(AlertEvent event) {
    s_eventBus.post(event);
  }

  /**
   * Register a listener to receive events. The listener should use the
   * {@link Subscribe} annotation.
   *
   * @param object
   *          the listener to receive events.
   */
  public void register(Object object) {
    s_eventBus.register(object);
  }

  /**
   * A custom {@link ThreadFactory} for the threads that will handle published
   * {@link AlertEvent}. Threads created will have slightly reduced priority
   * since {@link AlertEvent} instances are not critical to the system.
   */
  private static final class AlertEventBusThreadFactory implements
      ThreadFactory {

    private static final AtomicInteger s_threadIdPool = new AtomicInteger(1);

    /**
     * {@inheritDoc}
     */
    @Override
    public Thread newThread(Runnable r) {
      Thread thread = new Thread(r, "alert-event-bus-"
          + s_threadIdPool.getAndIncrement());

      thread.setDaemon(false);
      thread.setPriority(Thread.NORM_PRIORITY - 1);

      return thread;
    }
  }
}
