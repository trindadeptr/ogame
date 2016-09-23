package objects;

import java.util.HashMap;

/**
 * Created by jarndt on 9/23/16.
 */
public class Fleet {
    private HashMap<Ship,Integer> ships = new HashMap<>();


    @Override
    public String toString() {
        return "Fleet{" +
                "ships=" + ships +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Fleet fleet = (Fleet) o;

        return ships != null ? ships.equals(fleet.ships) : fleet.ships == null;

    }

    @Override
    public int hashCode() {
        return ships != null ? ships.hashCode() : 0;
    }

    public HashMap<Ship, Integer> getShips() {

        return ships;
    }

    public void setShips(HashMap<Ship, Integer> ships) {
        this.ships = ships;
    }
}
