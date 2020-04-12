package com.game.domain.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.game.domain.application.GameContext;
import com.game.domain.application.GameService;
import com.game.domain.application.GameServiceImpl;
import com.game.domain.application.command.InitializePlateauCommand;
import com.game.domain.application.command.InitializeRoverCommand;
import com.game.domain.application.command.MoveRoverCommand;
import com.game.domain.model.entity.Orientation;
import com.game.domain.model.entity.Plateau;
import com.game.domain.model.entity.Rover;
import com.game.domain.model.entity.RoverIdentifier;
import com.game.domain.model.entity.dimensions.RelativisticTwoDimensions;
import com.game.domain.model.entity.dimensions.TwoDimensionalCoordinates;
import com.game.domain.model.entity.dimensions.TwoDimensions;
import com.game.domain.model.exception.GameExceptionLabels;
import com.game.domain.model.exception.IllegalArgumentGameException;
import com.game.domain.model.exception.PlateauLocationAlreadySetException;
import com.game.domain.model.service.PlateauService;
import com.game.domain.model.service.RoverService;
import com.game.domain.model.service.ServiceLocator;

public class GameServiceImplTest {

	private static final int WIDTH = 5;

	private static final int HEIGHT = 5;

	private static final int X = 3;

	private static final int Y = 4;

	private GameContext gameContext = GameContext.getInstance();

	private GameService gameService = new GameServiceImpl();

	public List<Rover> roversList = new ArrayList<Rover>();

	public Plateau plateau;
	
	UUID relativisticUUID = UUID.fromString("53567a5d-a21c-495e-80a3-d12adaf8585c");

	@BeforeTest
	public void setup() {
		ServiceLocator mockServiceLocator = new ServiceLocator();
		mockServiceLocator.loadDomainService(ServiceLocator.ROVER_SERVICE, new MockRoverServiceImpl());
		mockServiceLocator.loadDomainService(ServiceLocator.PLATEAU_SERVICE, new MockPlateauServiceImpl());
		ServiceLocator.load(mockServiceLocator);
	}

	@BeforeMethod
	public void resetGame() {
		gameContext.reset();
		roversList.clear();
		((MockPlateauServiceImpl) gameContext.getPlateauService()).reset();
	}

	@Test
	public void testInitializePlateau() {
		gameService.execute(new InitializePlateauCommand.Builder().withUuid(UUID.randomUUID()).withAbscissa(WIDTH)
				.withOrdinate(HEIGHT).build());
		assertThat(plateau.getWidth()).isEqualTo(WIDTH);
		assertThat(plateau.getHeight()).isEqualTo(HEIGHT);
		assertThat(gameContext.getPlateau()).isEqualTo(plateau);
	}

	@Test
	public void testInitializeRelativisticPlateau() {
		gameService.execute(new InitializePlateauCommand.Builder().withUuid(UUID.randomUUID()).withAbscissa(WIDTH)
				.withOrdinate(HEIGHT).withObserverSpeed(2 * GameContext.MINIMAL_RELATIVISTIC_SPEED).build());
		assertThat(plateau.getWidth()).isEqualTo(WIDTH - 2);
		assertThat(plateau.getHeight()).isEqualTo(HEIGHT -2);
		assertThat(gameContext.getPlateau().getId()).isEqualTo(relativisticUUID);
	}

	@Test
	public void testInitializeRover() {
		UUID uuid = UUID.randomUUID();
		gameService.execute(new InitializePlateauCommand.Builder().withUuid(uuid).withAbscissa(WIDTH)
				.withOrdinate(HEIGHT).build());
		TwoDimensionalCoordinates coordinates = new TwoDimensionalCoordinates(X, Y);
		InitializeRoverCommand initializeCommand = new InitializeRoverCommand.Builder().withPlateauUuid(uuid)
				.withName(GameContext.ROVER_NAME_PREFIX + 1).withAbscissa(coordinates.getAbscissa())
				.withOrdinate(coordinates.getOrdinate()).withOrientation('S').build();
		gameService.execute(initializeCommand);
		assertThat(roversList.contains(new Rover(new RoverIdentifier(uuid, GameContext.ROVER_NAME_PREFIX + 1),
				coordinates, Orientation.SOUTH))).isTrue();
		assertThat(gameContext.getPlateauService().isLocationBusy(uuid, coordinates)).isTrue();
		TwoDimensionalCoordinates otherCoordinates = new TwoDimensionalCoordinates(X + 1, Y + 1);
		InitializeRoverCommand otherInitializeCommand = new InitializeRoverCommand.Builder().withPlateauUuid(uuid)
				.withName(GameContext.ROVER_NAME_PREFIX + 2).withAbscissa(otherCoordinates.getAbscissa())
				.withOrdinate(otherCoordinates.getOrdinate()).withOrientation('E').build();
		gameService.execute(otherInitializeCommand);
		assertThat(roversList.contains(new Rover(new RoverIdentifier(uuid, GameContext.ROVER_NAME_PREFIX + 2),
				otherCoordinates, Orientation.EAST))).isTrue();
		assertThat(gameContext.getPlateauService().isLocationBusy(uuid, otherCoordinates)).isTrue();
	}

	/**
	 * Expected error message: "[ERR-000] Missing Plateau configuration - It is not
	 * allowed to add a Rover. Please initialize the Plateau first."
	 */
	@Test
	public void testInitializeRoverWithoutPlateau() {
		InitializeRoverCommand initializeCommand = new InitializeRoverCommand.Builder().withAbscissa(X).withOrdinate(Y)
				.withName(GameContext.ROVER_NAME_PREFIX + 1).withOrientation('S').withPlateauUuid(UUID.randomUUID())
				.build();
		Throwable thrown = catchThrowable(() -> gameService.execute(initializeCommand));
		assertThat(thrown).isInstanceOf(IllegalArgumentGameException.class)
				.hasMessage(String.format(GameExceptionLabels.ERROR_CODE_AND_MESSAGE_PATTERN,
						GameExceptionLabels.ILLEGAL_ARGUMENT_CODE,
						String.format(GameExceptionLabels.ERROR_MESSAGE_SEPARATION_PATTERN,
								GameExceptionLabels.MISSING_PLATEAU_CONFIGURATION,
								GameExceptionLabels.ADDING_ROVER_NOT_ALLOWED)));
	}

	@Test
	public void testMoveRoverWithOrientation() {
		UUID uuid = UUID.randomUUID();
		String roverName = GameContext.ROVER_NAME_PREFIX + 3;
		gameService.execute(new MoveRoverCommand(uuid, roverName, 1));
		assertThat(roversList).contains(
				new Rover(new RoverIdentifier(uuid, roverName), new TwoDimensionalCoordinates(2, 3), Orientation.WEST));

	}

	/**
	 * Here we test that no exception is caught in GameServiceImpl method
	 */
	@Test
	public void testMoveRoverWithOrientationOutOfTheBoard() {
		String roverName = GameContext.ROVER_NAME_PREFIX + 5;
		Throwable thrown = catchThrowable(
				() -> gameService.execute(new MoveRoverCommand(UUID.randomUUID(), roverName, 1)));
		assertThat(thrown).isInstanceOf(PlateauLocationAlreadySetException.class)
				.hasMessage(String.format(GameExceptionLabels.ERROR_CODE_AND_MESSAGE_PATTERN,
						GameExceptionLabels.PLATEAU_LOCATION_ERROR_CODE, "Error"));

	}

	/**
	 * Simple MockClass for the RoverServiceImpl
	 *
	 */
	private class MockRoverServiceImpl implements RoverService {

		@Override
		public void initializeRover(RoverIdentifier id, TwoDimensionalCoordinates coordinates,
				Orientation orientation) {
			GameServiceImplTest.this.roversList
					.add(new Rover(new RoverIdentifier(id.getPlateauUuid(), id.getName()), coordinates, orientation));
		}

		@Override
		public void faceToOrientation(RoverIdentifier id, Orientation orientation) {

		}

		@Override
		public void moveRoverNumberOfTimes(RoverIdentifier id, int numberOfTimes) {
			GameServiceImplTest.this.roversList.add(new Rover(new RoverIdentifier(id.getPlateauUuid(), id.getName()),
					new TwoDimensionalCoordinates(2, 3), Orientation.WEST));
			if (id.getName().equals(GameContext.ROVER_NAME_PREFIX + 5))
				throw new PlateauLocationAlreadySetException("Error");
		}

		@Override
		public void updateRover(Rover rover) {
		}

		@Override
		public Rover getRover(RoverIdentifier id) {
			return null;
		}

		@Override
		public List<Rover> getAllRoversOnPlateau(UUID uuid) {
			return null;
		}

		@Override
		public void updateRoverWithPosition(RoverIdentifier id, TwoDimensionalCoordinates position) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void updateRoverWithOrientation(RoverIdentifier id, Orientation orientation) {
			// TODO Auto-generated method stub
			
		}

	}

	/**
	 * Simple MockClass for the PlateauServiceImpl
	 *
	 */
	private class MockPlateauServiceImpl implements PlateauService {

		Map<TwoDimensionalCoordinates, Boolean> mapLocations = new HashMap<>();

		@Override
		public Plateau initializePlateau(UUID uuid, TwoDimensionalCoordinates coordinates) {
			GameServiceImplTest.this.plateau = new Plateau(uuid,
					new TwoDimensions(
							new TwoDimensionalCoordinates(coordinates.getAbscissa(), coordinates.getOrdinate())))
									.initializeLocations();
			return GameServiceImplTest.this.plateau;
		}

		/**
		 * What ever UUID we pass as argument, if we go through this method we return the relativistic UUID back
		 */
		@Override
		public Plateau initializeRelativisticPlateau(UUID uuid, int speed, TwoDimensionalCoordinates coordinates) {
			GameServiceImplTest.this.plateau = new Plateau(relativisticUUID,
					new RelativisticTwoDimensions(speed, new TwoDimensions(
							new TwoDimensionalCoordinates(coordinates.getAbscissa(), coordinates.getOrdinate()))))
									.initializeLocations();
			return GameServiceImplTest.this.plateau;
		}

		@Override
		public void updatePlateauWithBusyLocation(UUID uuid, TwoDimensionalCoordinates coordinates) {
			mapLocations.put(coordinates, Boolean.TRUE);
		}

		@Override
		public boolean isLocationBusy(UUID uuid, TwoDimensionalCoordinates coordinates) {
			return mapLocations.get(coordinates);
		}

		@Override
		public void updatePlateauWithFreeLocation(UUID uuid, TwoDimensionalCoordinates coordinates) {
			mapLocations.put(coordinates, Boolean.FALSE);

		}

		@Override
		public Plateau loadPlateau(UUID plateauUuid) {
			return GameServiceImplTest.this.plateau;
		}

		public void reset() {
			GameServiceImplTest.this.plateau = null;
		}

		@Override
		public void updatePlateau(Plateau plateau) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void updatePlateauWithLocations(UUID plateauUUID, TwoDimensionalCoordinates freeLocation,
				TwoDimensionalCoordinates busyLocation) {
			// TODO Auto-generated method stub
			
		}

	}

}
