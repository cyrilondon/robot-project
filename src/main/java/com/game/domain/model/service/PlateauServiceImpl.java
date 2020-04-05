package com.game.domain.model.service;

import com.game.domain.model.entity.Plateau;
import com.game.domain.model.entity.dimensions.RelativisticTwoDimensions;
import com.game.domain.model.entity.dimensions.TwoDimensionalCoordinates;
import com.game.domain.model.entity.dimensions.TwoDimensions;
import com.game.domain.model.validation.EntityDefaultValidationNotificationHandler;

/**
 * Domain service which has the responsibility to handle the entity
 * {@link Plateau}
 *
 */
public class PlateauServiceImpl implements DomainService {

	public Plateau initializePlateau(TwoDimensionalCoordinates coordinates) {
		Plateau plateau =  new Plateau(new TwoDimensions(
				new TwoDimensionalCoordinates(coordinates.getAbscissa(), coordinates.getOrdinate())));
		return plateau.validate(new EntityDefaultValidationNotificationHandler());
	}

	/**
	 * Initializes the plateau as observed from an observer with speed v
	 * 
	 * @param speed       observer speed
	 * @param coordinates with rest dimensions
	 * @return relativistic plateau
	 */
	public Plateau initializeRelativisticPlateau(int speed, TwoDimensionalCoordinates coordinates) {
		Plateau plateau = new Plateau(new RelativisticTwoDimensions(speed,
				(new TwoDimensionalCoordinates(coordinates.getAbscissa(), coordinates.getOrdinate()))));
		return plateau.validate(new EntityDefaultValidationNotificationHandler());
	}

}
