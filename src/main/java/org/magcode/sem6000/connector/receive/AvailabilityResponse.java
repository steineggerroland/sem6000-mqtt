package org.magcode.sem6000.connector.receive;

public class AvailabilityResponse extends SemResponse {

	private final Availability availability;

	public enum Availability {
		LOST, AVAILABLE
	}

	public AvailabilityResponse(Availability available) {
		super(ResponseType.AVAILABILITY);
		this.availability = available;
	}

	public Availability getAvailability() {
		return availability;
	}

	@Override
	public String toString() {
		return "Device is " + ((availability == Availability.AVAILABLE) ? "online" : "offline");
	}
}