package pw.geckonerd.cursedrbloader;

public interface Event {
	String getEventID();
	Boolean isBefore();
	Boolean isAfter();
}
