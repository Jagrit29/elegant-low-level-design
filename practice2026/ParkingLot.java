package practice2026;

/*
Requirements
Design PArking lot System 
1. Parking lot can have multiple levels 
2. Each parking level can have multiple parking spots 
3. Parking spot can have be for car, bike, truck (Vehicle type)
4. 

Core Entities 
1. ParkingSpot - id, parkingSpotType, vechile
2. Vehcile -> id, vechileType
3. ParkingLot -> Level, ParkingSpot[]
4. Parking

 */

enum VechileType {
    SMALL,
    MEDIUM,
    LARGE
}

enum SpotType {
    SMALL, 
    MEDIUM, 
    LARGE
}

class Vechile {
    String registrationNumber;
    VechileType vechileType;

    public Vechile(String reg, VechileType vechileType) {
        this.registrationNumber = reg;
        this.vechileType = vechileType;
    }
}


class ParkingSpot {
    String id;
    Vechile parkedVechile;
    SpotType spotType;

    public ParkingSpot(String id, SpotType spotType) {
        this.id = id;
        this.spotType = spotType;
    }

    public boolean isAvailable() {
        return parkedVechile == null;
    }

    public void park(Vechile vechile) {
        // have a check of vechile type here;
        // we can have another check for canFIT etc
        parkedVechile = vechile;
    }

    public void unpark() {
        parkedVechile = null;
    }

}


public class ParkingLot {
    
}
