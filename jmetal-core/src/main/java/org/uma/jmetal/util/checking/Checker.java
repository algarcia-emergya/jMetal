package org.uma.jmetal.util.checking;

import org.uma.jmetal.util.checking.exception.*;

import java.util.Collection;

public class Checker {
  public static void isNotNull(Object object) {
    if (null == object) {
      throw new NullParameterException() ;
    }
  }

  public static void isValidProbability(double value) {
    if ((value < 0.0) || (value > 1.0)) {
      throw new InvalidProbabilityValueException(value) ;
    }
  }

  public static void valueIsInRange(double value, double lowestValue, double highestValue) {
    if ((value < lowestValue) || (value > highestValue)) {
      throw new ValueOutOfRangeException(value, lowestValue, highestValue) ;
    }
  }

  public static void valueIsInRange(int value, int lowestValue, int highestValue) {
    if ((value < lowestValue) || (value > highestValue)) {
      throw new ValueOutOfRangeException(value, lowestValue, highestValue) ;
    }
  }

  public static void collectionIsNotEmpty(Collection<?> collection) {
    if (collection.isEmpty()) {
      throw new EmptyCollectionException() ;
    }
  }

  public static void isTrue(boolean expression, String message) {
    if (!expression) {
      throw new RuntimeException(message);
    }
  }

  public static void that(boolean expression, String message) {
    if (!expression) {
        throw new InvalidConditionException(message) ;
    }
  }
}
