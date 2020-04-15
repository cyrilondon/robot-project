package com.game.domain.application.command;

import com.game.domain.application.CommandVisitor;
import com.game.domain.model.entity.RoverIdentifier;

/**
 * Command sent from the client to move a Rover with given name
 * by a certain number of moves
 *
 */
public class MoveRoverCommand implements ApplicationCommand {

	private RoverIdentifier roverId;
	
	private int numberOfMoves;
	
	public MoveRoverCommand(RoverIdentifier roverId, int numberOfMoves) {
		this.roverId = roverId;
		this.numberOfMoves = numberOfMoves;
	}

	public RoverIdentifier getRoverId() {
		return roverId;
	}

	public int getNumberOfMoves() {
		return numberOfMoves;
	}

	@Override
	public void acceptVisitor(CommandVisitor visitor) {
		visitor.visit(this);
	}
	
}
