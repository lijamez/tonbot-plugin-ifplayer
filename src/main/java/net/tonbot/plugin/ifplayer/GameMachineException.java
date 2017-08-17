package net.tonbot.plugin.ifplayer;

@SuppressWarnings("serial")
class GameMachineException extends RuntimeException {

	public GameMachineException(String message) {
		super(message);
	}
	
	public GameMachineException(String message, Exception causedBy) {
		super(message, causedBy);
	}
	
}
