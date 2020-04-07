package com.game.domain.model;

public interface DomainEventSubscriber<T> {

	public void handleEvent(T event);

	public Class<T> subscribedToEventType();

}
