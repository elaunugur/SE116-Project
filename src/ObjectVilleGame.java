import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

public class ObjectVilleGame {
    public static void main(String[] args) {
        String mapFile;
        if(args.length >= 1){
            mapFile = args[0];
        }else{
            mapFile = "map00.txt";
        }

        int ticks;
        if(args.length >= 2){
            ticks = Integer.parseInt(args[1]);
        }else{
            ticks = 10;
        }

        Building[][] mainArray = MapLoader.loadFromFile(mapFile);

        City city = new City(mainArray);

        for(int i = 1; i <= ticks; i++){
            city.tick(i);
        }
    }
}

class City {

    Building[][] grid;
    private final int rows;
    private final int cols;

    private int populationStorage;
    private int goodsStorage;
    private int lifestyleStorage;


    private int nextPopulationStorage;
    private int nextGoodsStorage;
    private int nextLifestyleStorage;

    City(Building[][] grid) {
        this.grid = grid;
        this.rows = grid.length;
        if (this.rows > 0) {
            this.cols = grid[0].length;
        } else {
            this.cols = 0;
        }
    }
    private void forEachBuilding(BuildingAction action) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (grid[i][j] != null) {

                    action.apply(grid[i][j]);
                }
            }
        }
    }

    public boolean coordinateChecker(int userRow, int userCol) {
        return userRow >= 0 && userRow < rows && userCol >= 0 && userCol < cols;
    }

    public Building buildingInThatCoordinate(int userRow, int userCol) {
        if (coordinateChecker(userRow, userCol)) {
            return grid[userRow][userCol];
        }
        return null;
    }

    public void tick(int tickNumber) {
        System.out.println("Tick " + tickNumber);

        populationStorage = nextPopulationStorage;
        goodsStorage = nextGoodsStorage;
        lifestyleStorage = nextLifestyleStorage;

        nextPopulationStorage = 0;
        nextGoodsStorage = 0;
        nextLifestyleStorage = 0;


        forEachBuilding(b -> b.resetForNewTick());


        forEachBuilding(b -> {
            if (b instanceof ServiceProvider) {
                ((ServiceProvider) b).applyServices(this);
            }
        });


        forEachBuilding(b -> {
            if (b instanceof UtilityProvider) {
                ((UtilityProvider) b).distributeUtilities(this);
            }
        });

        distributeResources();


        forEachBuilding(b -> {
            if (b instanceof Upgrade) {
                ((Upgrade) b).updateBuilding(this);
            }
        });


        forEachBuilding(b -> {
            if (b instanceof ResourceProducer) {
                ((ResourceProducer) b).calculateProduction(this);
            }
        });
    }

    private void distributeResources() {

        int numberOfHouse = 0;
        int numberOfIndustrial = 0;
        int numberOfCommercial = 0;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (grid[row][col] instanceof House) {
                    numberOfHouse++;
                } else if (grid[row][col] instanceof Industrial) {
                    numberOfIndustrial++;
                } else if (grid[row][col] instanceof Commercial) {
                    numberOfCommercial++;
                }
            }
        }

        int sharedPopulation = 0;
        int numberOfPopulationReceivers = numberOfIndustrial + numberOfCommercial;
        if (populationStorage > 0 && numberOfPopulationReceivers > 0) {
            sharedPopulation = populationStorage / numberOfPopulationReceivers;
        }

        int sharedGoods = 0;
        if (goodsStorage > 0 && numberOfCommercial > 0) {
            sharedGoods = goodsStorage / numberOfCommercial;
        }

        int sharedLifeStyle = 0;
        if (lifestyleStorage > 0 && numberOfHouse > 0) {
            sharedLifeStyle = lifestyleStorage / numberOfHouse;
        }

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {

                Building building = grid[row][col];

                if (building instanceof House) {
                    House house = (House) building;

                    if (sharedLifeStyle > 0) {
                        house.setCurrentLifestyle(sharedLifeStyle);
                        System.out.println("House at (" + row + "," + col + ") received " + sharedLifeStyle + " lifestyle");
                    }

                } else if (building instanceof Commercial) {
                    Commercial commercial = (Commercial) building;

                    if (sharedPopulation > 0) {
                        commercial.setCurrentPopulation(sharedPopulation);
                        System.out.println("Commercial at (" + row + "," + col + ") received " + sharedPopulation + " population");
                    }

                    if (sharedGoods > 0) {
                        commercial.setCurrentGoods(sharedGoods);
                        System.out.println("Commercial at (" + row + "," + col + ") received " + sharedGoods + " goods");
                    }

                } else if (building instanceof Industrial) {
                    Industrial industrial = (Industrial) building;

                    if (sharedPopulation > 0) {
                        industrial.setCurrentPopulation(sharedPopulation);
                        System.out.println("Industrial at (" + row + "," + col + ") received " + sharedPopulation + " population");
                    }
                }
            }
        }
    }

    public void deliverBFS(int startRow, int startCol, String utilityType, int capacity) {

        boolean[][] hasExploredLocation = new boolean[rows][cols];
        ArrayList<int[]> coordinates = new ArrayList<int[]>();



        hasExploredLocation[startRow][startCol] = true;
        addNeighborsToList(startRow, startCol, coordinates, hasExploredLocation);

        while (coordinates.size() > 0 && capacity > 0) {

            int[] currentPosition = coordinates.remove(0);
            int currentRow = currentPosition[0];
            int currentCol = currentPosition[1];

            Building building = grid[currentRow][currentCol];

            if (building instanceof House || building instanceof Industrial || building instanceof Commercial) {

                if (utilityType.equals("internet") && building instanceof Industrial) {
                    addNeighborsToList(currentRow, currentCol, coordinates, hasExploredLocation);
                    continue;
                }

                int alreadyReceived = 0;

                if (utilityType.equals("electricity")) {
                    alreadyReceived = building.getElectricity();
                } else if (utilityType.equals("water")) {
                    alreadyReceived = building.getWater();
                } else if (utilityType.equals("internet")) {
                    alreadyReceived = building.getInternet();
                }

                int remainingDemand = building.demand - alreadyReceived;

                if (remainingDemand < 0) {
                    remainingDemand = 0;
                }

                if (remainingDemand > 0) {

                    int give = remainingDemand;

                    if (give > capacity) {
                        give = capacity;
                    }

                    if (utilityType.equals("electricity")) {
                        building.calculateReceivedElectricity(give);
                    } else if (utilityType.equals("water")) {
                        building.calculateReceivedWater(give);
                    } else if (utilityType.equals("internet")) {
                        building.calculateReceivedInternet(give);
                    }

                    String zoneName = "";

                    if (building instanceof House) {
                        zoneName = "House";
                    } else if (building instanceof Industrial) {
                        zoneName = "Industrial";
                    } else if (building instanceof Commercial) {
                        zoneName = "Commercial";
                    }

                    System.out.println(zoneName + " at (" + currentRow + "," + currentCol + ") received " + give + " " + utilityType);

                    capacity -= give;
                }
            }

            addNeighborsToList(currentRow, currentCol, coordinates, hasExploredLocation);
        }
    }

    private void addNeighborsToList(int row, int col, ArrayList<int[]> coordinates, boolean[][] visited) {

        if (row - 1 >= 0 && grid[row - 1][col] != null && grid[row - 1][col] instanceof Connectable) {
            if (((Connectable) grid[row - 1][col]).canConnect() && !visited[row - 1][col]) {
                visited[row - 1][col] = true;
                coordinates.add(new int[]{row - 1, col});
            }
        }

        if (row + 1 < rows && grid[row + 1][col] != null && grid[row + 1][col] instanceof Connectable) {
            if (((Connectable) grid[row + 1][col]).canConnect() && !visited[row + 1][col]) {
                visited[row + 1][col] = true;
                coordinates.add(new int[]{row + 1, col});
            }
        }

        if (col - 1 >= 0 && grid[row][col - 1] != null && grid[row][col - 1] instanceof Connectable) {
            if (((Connectable) grid[row][col - 1]).canConnect() && !visited[row][col - 1]) {
                visited[row][col - 1] = true;
                coordinates.add(new int[]{row, col - 1});
            }
        }

        if (col + 1 < cols && grid[row][col + 1] != null && grid[row][col + 1] instanceof Connectable) {
            if (((Connectable) grid[row][col + 1]).canConnect() && !visited[row][col + 1]) {
                visited[row][col + 1] = true;
                coordinates.add(new int[]{row, col + 1});
            }
        }
    }

    public void addToPopulationStorage(int amount) {
        nextPopulationStorage += amount;
    }

    public void addToGoodsStorage(int amount) {
        nextGoodsStorage += amount;
    }

    public void addToLifestyleStorage(int amount) {
        nextLifestyleStorage += amount;
    }
}

class MapLoader {

    public static Building[][] loadFromFile(String nameOfFile){

        try{

            File readFile = new File(nameOfFile);

            Scanner sizeReader = new Scanner(readFile);

            int rowCount = 0;
            int colCount = 0;

            while(sizeReader.hasNextLine()){

                String line = sizeReader.nextLine();

                rowCount++;

                if(line.length() > colCount){
                    colCount = line.length();
                }
            }

            sizeReader.close();

            Building[][] map = new Building[rowCount][colCount];

            Scanner reader = new Scanner(readFile);

            int row = 0;

            while(reader.hasNextLine()){

                String nextRow = reader.nextLine();

                for(int col = 0 ; col < nextRow.length(); col++ ){

                    char eachLetter = nextRow.charAt(col);

                    switch (eachLetter){

                        case 'E':
                            map[row][col] = new Empty(row,col);
                            break;

                        case 'H':
                            map[row][col] = new House(row,col);
                            break;

                        case 'R':
                            map[row][col] = new Road(row,col);
                            break;

                        case 'C':
                            map[row][col] = new Commercial(row,col);
                            break;

                        case 'I':
                            map[row][col] = new Industrial(row,col);
                            break;

                        case 'S':
                            map[row][col] = new School(row,col);
                            break;

                        case 'F':
                            map[row][col] = new PoliceStation(row,col);
                            break;

                        case 'D':
                            map[row][col] = new Hospital(row,col);
                            break;

                        case 'P':
                            map[row][col] = new PowerPlant(row,col);
                            break;

                        case 'W':
                            map[row][col] = new WaterPlant(row,col);
                            break;

                        case 'T':
                            map[row][col] = new InternetProvider(row,col);
                            break;
                    }
                }

                row++;
            }

            reader.close();

            return map;

        } catch (FileNotFoundException e ) {

            System.out.println("The document has not been found!");

            return new Building[0][0];
        }
    }
}
abstract class Building{}

class School extends Building implements ServiceProvider {}

class PoliceStation extends Building implements ServiceProvider {}

class Hospital extends Building implements ServiceProvider {}

class House extends Building implements Connectable, Upgrade, ResourceProducer {}

class Industrial extends Building implements Connectable, Upgrade, ResourceProducer {}

class Commercial extends Building implements Connectable, Upgrade, ResourceProducer {}

class PowerPlant extends Building implements UtilityProvider {}

class WaterPlant extends Building implements UtilityProvider {}

class InternetProvider extends Building implements UtilityProvider {}

class Empty extends Building {

    public Empty(int row, int col) {
        super(row, col, 'E', "Empty");
    }

}


class Road extends Building implements Connectable {

    public Road(int row, int col) {
        super(row, col, 'R', "Road");
    }

    @Override
    public boolean canConnect() {
        return true;
    }
}